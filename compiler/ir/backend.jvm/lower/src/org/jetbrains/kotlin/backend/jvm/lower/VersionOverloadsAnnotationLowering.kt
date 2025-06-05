/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
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
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_INTRODUCED_AT_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import java.util.SortedMap

@PhaseDescription(
    name = "VersionOverloadsAnnotation"
)
internal class VersionOverloadsAnnotationLowering(val context: JvmBackendContext) : ClassLoweringPass {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns
    private val deprecationBuilder = DeprecationBuilder(context, level=DeprecationLevel.HIDDEN)

    companion object {
        private val COPY_METHOD_NAME = Name.identifier("copy")
    }

    override fun lower(irClass: IrClass) {
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        val loweringContext = LoweringContext(irClass)
        functions.forEach { generateVersionOverloads(it, loweringContext) }
    }


    class LoweringContext(
        val irClass: IrClass,
        var copyMethodVersions: SortedMap<MavenComparableVersion?, MutableList<Int>>? = null
    )

    fun generateVersionOverloads(target: IrFunction, loweringContext: LoweringContext) {
        val isDataClass = loweringContext.irClass.isData
        val versionParamIndexes =
            if (isDataClass && target.name == COPY_METHOD_NAME) {
                loweringContext.copyMethodVersions
            } else {
                getSortedVersionParameterIndexes(target)
            }

        generateVersions(target, loweringContext.irClass, versionParamIndexes)

        if (isDataClass && target is IrConstructor && target.isPrimary && versionParamIndexes != null) {
            versionParamIndexes.forEach { entry ->
                for (i in entry.value.indices) {
                    entry.value[i] += 1
                }
            }

            versionParamIndexes[null]?.add(0)
            loweringContext.copyMethodVersions = versionParamIndexes
        }
    }

    private fun generateVersions(
        func: IrFunction,
        containingClass: IrClass,
        versionParamIndexes: SortedMap<MavenComparableVersion?, MutableList<Int>>?
    ) {
        if (versionParamIndexes == null || versionParamIndexes.size < 2) return

        val lastIncludedParameters = BooleanArray(func.parameters.size) { true }

        versionParamIndexes.asIterable().forEachIndexed { i, (_, paramIndexes) ->
            if (i > 0) {
                containingClass.addMember(generateWrapper(func, lastIncludedParameters))
            }

            paramIndexes.forEach {
                lastIncludedParameters[it] = false
            }
        }
    }

    private fun getSortedVersionParameterIndexes(func: IrFunction): SortedMap<MavenComparableVersion?, MutableList<Int>> {
        val versionIndexes = sortedMapOf<MavenComparableVersion?, MutableList<Int>>(nullsLast(compareByDescending { it }))

        func.parameters.forEachIndexed { i, param ->
            val versionNumber = param.getVersionNumber()

            if (versionIndexes.containsKey(versionNumber)) {
                versionIndexes[versionNumber]!!.add(i)
            } else {
                versionIndexes[versionNumber] = mutableListOf(i)
            }
        }

        return versionIndexes
    }

    private fun IrValueParameter.getVersionNumber() : MavenComparableVersion? {
        if (kind != IrParameterKind.Regular || defaultValue == null) return null
        val annotation = getAnnotation(JVM_INTRODUCED_AT_FQ_NAME) ?: return null
        val versionString = (annotation.arguments.first() as? IrConst)?.value as? String ?: return null

        return MavenComparableVersion(versionString)
    }

    private fun generateWrapper(original: IrFunction, includedParams: BooleanArray): IrFunction {
        val wrapperIrFunction = irFactory.generateWrapperHeader(original, includedParams)

        val call = when (original) {
            is IrConstructor ->
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, irBuiltIns.unitType, original.symbol
                )
            is IrSimpleFunction ->
                IrCallImpl.fromSymbolOwner(
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
            is IrConstructor -> {
                irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(call))
            }
            is IrSimpleFunction -> {
                irFactory.createExpressionBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, call)
            }
        }

        return wrapperIrFunction
    }

    private fun IrFactory.generateWrapperHeader(original: IrFunction, includedParams: BooleanArray): IrFunction {
        return when (original) {
            is IrConstructor -> {
                buildConstructor {
                    setSourceRange(original)
                    origin = JvmLoweredDeclarationOrigin.VERSION_OVERLOAD_WRAPPER
                    name = original.name
                    visibility = original.visibility
                    returnType = original.returnType
                    isInline = original.isInline
                    containerSource = original.containerSource
                }
            }
            is IrSimpleFunction -> buildFun {
                setSourceRange(original)
                origin = JvmLoweredDeclarationOrigin.VERSION_OVERLOAD_WRAPPER
                name = original.name
                visibility = original.visibility
                modality = original.modality
                returnType = original.returnType
                isInline = original.isInline
                isSuspend = original.isSuspend
                containerSource = original.containerSource
            }
        }.apply {
            parent = original.parent
            setOverloadAnnotationsWith(original)
            copyTypeParametersFrom(original)
            generateNewValueParameters(original, includedParams)
        }
    }

    private fun IrFunction.setOverloadAnnotationsWith(source: IrFunction) {
        annotations = annotations memoryOptimizedPlus
                deprecationBuilder.buildAnnotationCall() memoryOptimizedPlus
                source.copyNonJvmOverloadsAnnotations()
    }

    private fun IrAnnotationContainer.copyNonJvmOverloadsAnnotations(): List<IrConstructorCall> =
        annotations.memoryOptimizedMapNotNull {
            if (it.isAnnotation(JVM_OVERLOADS_FQ_NAME))
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

    private class DeprecationBuilder(private val context: JvmBackendContext, level: DeprecationLevel) {
        private val classSymbol = context.irPluginContext?.referenceClass(StandardClassIds.Annotations.Deprecated)!!
        private val deprecationLevelClass = context.irPluginContext?.referenceClass(StandardClassIds.DeprecationLevel)!!
        private val levelSymbol = deprecationLevelClass.owner.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.toString() == level.name }.symbol

        fun buildAnnotationCall() : IrConstructorCall {
            return IrConstructorCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                classSymbol.defaultType,
                classSymbol.constructors.first()
            ).apply {
                arguments[0] =
                    IrConstImpl.string(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                        context.irBuiltIns.stringType, "Deprecated"
                    )

                arguments[2] =
                    IrGetEnumValueImpl(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                        deprecationLevelClass.defaultType, levelSymbol
                    )
            }
        }
    }
}
