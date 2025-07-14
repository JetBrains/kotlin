/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.ir.util.remapSymbolParent
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import java.util.SortedMap

@PhaseDescription(
    name = "VersionOverloadsLowering"
)
class VersionOverloadsLowering(val context: LoweringContext) : ClassLoweringPass {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns

    override fun lower(irClass: IrClass) {
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        val copyMethodVersions = CopyMethodVersions()
        functions.forEach { generateVersionOverloads(it, irClass, copyMethodVersions) }
    }

    class CopyMethodVersions(
        var versions: SortedMap<MavenComparableVersion?, MutableList<Int>>? = null,
    )

    fun generateVersionOverloads(target: IrFunction, irClass: IrClass, copyMethodVersions: CopyMethodVersions) {
        val versionParamIndexes = when {
            irClass.isData && target.name == StandardNames.DATA_CLASS_COPY -> copyMethodVersions.versions
            else -> getSortedVersionParameterIndexes(target)
        }

        generateVersions(target, irClass, versionParamIndexes)

        if (irClass.isData && target is IrConstructor && target.isPrimary && versionParamIndexes != null) {
            versionParamIndexes.forEach { (_, params) -> params.indices.forEach { params[it] += 1 } }
            versionParamIndexes[null]?.add(0)
            copyMethodVersions.versions = versionParamIndexes
        }
    }

    private fun generateVersions(
        func: IrFunction,
        containingClass: IrClass,
        versionParamIndexes: SortedMap<MavenComparableVersion?, MutableList<Int>>?
    ) {
        if (versionParamIndexes == null || versionParamIndexes.size < 2) return

        val lastIncludedParameters = BooleanArray(func.parameters.size) { true }

        var first = true
        versionParamIndexes.forEach { (_, params) ->
            when {
                first -> first = false
                else -> containingClass.addMember(generateWrapper(func, lastIncludedParameters))
            }
            params.forEach { lastIncludedParameters[it] = false }
        }
    }

    private fun getSortedVersionParameterIndexes(func: IrFunction): SortedMap<MavenComparableVersion?, MutableList<Int>> =
        sortedMapOf<MavenComparableVersion?, MutableList<Int>>(nullsLast(compareByDescending { it }))
            .apply {
                func.parameters.forEachIndexed { i, param ->
                    val versionNumber = param.getVersionNumber()
                    putIfAbsent(versionNumber, mutableListOf())
                    this[versionNumber]!!.add(i)
                }
            }

    private fun IrValueParameter.getVersionNumber(): MavenComparableVersion? {
        if (kind != IrParameterKind.Regular || defaultValue == null) return null
        val annotation = getAnnotation(StandardClassIds.Annotations.IntroducedAt.asSingleFqName()) ?: return null
        val versionString = (annotation.arguments.first() as? IrConst)?.value as? String ?: return null

        return MavenComparableVersion(versionString)
    }

    private fun generateWrapper(original: IrFunction, includedParams: BooleanArray): IrFunction {
        val wrapperIrFunction = irFactory.generateWrapperHeader(original, includedParams)

        val call = when (original) {
            is IrConstructor -> IrDelegatingConstructorCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, irBuiltIns.unitType, original.symbol
            )
            is IrSimpleFunction -> IrCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, original.returnType, original.symbol
            )
        }

        for (arg in wrapperIrFunction.allTypeParameters) {
            call.typeArguments[arg.index] = arg.defaultType
        }

        var lastWrapperIndex = 0
        for (originalIndex in original.parameters.indices) {
            if (!includedParams[originalIndex]) {
                call.arguments[originalIndex] = null
                continue
            }

            val wrapperParam = wrapperIrFunction.parameters[lastWrapperIndex]
            call.arguments[originalIndex] =
                IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, wrapperParam.symbol)
            lastWrapperIndex += 1
        }

        wrapperIrFunction.body = when (original) {
            is IrConstructor -> irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(call))
            else -> irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET) {
                statements += IrReturnImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    wrapperIrFunction.returnType,
                    wrapperIrFunction.symbol,
                    call
                )
            }
        }

        return wrapperIrFunction
    }

    private fun IrFactory.generateWrapperHeader(original: IrFunction, includedParams: BooleanArray): IrFunction {
        val builder = when (original) {
            is IrConstructor -> ::buildConstructor
            else -> ::buildFun
        }
        return builder {
            updateFrom(original)
            name = original.name
            origin = IrDeclarationOrigin.VERSION_OVERLOAD_WRAPPER
            returnType = original.returnType
        }.apply {
            parent = original.parent
            setOverloadAnnotationsWith(original)
            copyTypeParametersFrom(original)
            generateNewValueParameters(original, includedParams)
        }
    }

    private fun IrFunction.setOverloadAnnotationsWith(source: IrFunction) {
        annotations = annotations memoryOptimizedPlus
                buildDeprecationCall(DeprecationLevel.HIDDEN) memoryOptimizedPlus
                source.copyNonJvmOverloadsAnnotations()
    }

    private fun IrAnnotationContainer.copyNonJvmOverloadsAnnotations(): List<IrConstructorCall> =
        annotations.memoryOptimizedMapNotNull {
            if (it.isAnnotation(StandardClassIds.Annotations.jvmOverloads.asSingleFqName()))
                null
            else
                it.transform(DeepCopyIrTreeWithSymbols(SymbolRemapper.EMPTY), null) as IrConstructorCall
        }

    private fun IrFunction.generateNewValueParameters(original: IrFunction, includedParams: BooleanArray) {
        val originalDefaults = mutableListOf<IrExpressionBody?>()
        parameters = original.parameters.withIndex().mapNotNull { (i, param) ->
            if (!includedParams[i]) null
            else {
                originalDefaults.push(param.defaultValue)
                param.copyTo(this, defaultValue = null)
            }
        }

        // copy the value params first before the default values. required when there are default expressions that depend on other value params

        val transformer = GetValueTransformer(this)
        parameters.forEachIndexed { i, param ->
            val originalDefault = originalDefaults[i] ?: return@forEachIndexed

            param.defaultValue = factory.createExpressionBody(
                startOffset = originalDefault.startOffset,
                endOffset = originalDefault.endOffset,
                expression = originalDefault.expression.deepCopyWithSymbols(this),
            ).transform(transformer, null)
        }

    }

    private class GetValueTransformer(val irFunction: IrFunction) : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrGetValue {
            return (super.visitGetValue(expression) as IrGetValue).remapSymbolParent(
                classRemapper = { irFunction.parent as? IrClass ?: it },
                functionRemapper = { irFunction }
            )
        }
    }

    @OptIn(InternalSymbolFinderAPI::class)
    fun buildDeprecationCall(level: DeprecationLevel): IrConstructorCall {
        val levelSymbol = context.irBuiltIns.deprecationLevelSymbol.owner.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.toString() == level.name }.symbol

        return IrConstructorCallImpl.fromSymbolOwner(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            context.irBuiltIns.deprecatedSymbol.defaultType,
            context.irBuiltIns.deprecatedSymbol.constructors.first()
        ).apply {
            arguments[0] =
                IrConstImpl.string(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    context.irBuiltIns.stringType, "Deprecated"
                )
            arguments[2] =
                IrGetEnumValueImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    context.irBuiltIns.deprecationLevelSymbol.defaultType, levelSymbol
                )
        }
    }
}
