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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import java.util.*

@PhaseDescription(
    name = "VersionOverloadsLowering"
)
class VersionOverloadsLowering(val context: LoweringContext) : ClassLoweringPass {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns

    override fun lower(irClass: IrClass) {
        for (function in irClass.declarations.filterIsInstance<IrConstructor>()) {
            generateVersionOverloads(function, irClass)
        }
        for (function in irClass.declarations.filterIsInstance<IrSimpleFunction>()) {
            generateVersionOverloads(function, irClass)
        }
    }

    fun generateVersionOverloads(target: IrFunction, irClass: IrClass) {
        val versionParamIndexes = when {
            target is IrSimpleFunction && irClass.isData && target.name == StandardNames.DATA_CLASS_COPY -> {
                val primaryConstructor = irClass.declarations.filterIsInstance<IrConstructor>().single {
                    it.isPrimary && it.origin != IrDeclarationOrigin.VERSION_OVERLOAD_WRAPPER
                }
                // adjust the information from the primary constructor into that of 'copy'
                getSortedVersionParameterIndexes(primaryConstructor).also {
                    it.forEach { (_, params) -> params.indices.forEach { ix -> params[ix] += 1 } }
                    it[null]?.add(0)
                }
            }
            else -> getSortedVersionParameterIndexes(target)
        }

        generateVersions(target, irClass, versionParamIndexes)
    }

    private fun getSortedVersionParameterIndexes(function: IrFunction): SortedMap<MavenComparableVersion?, MutableList<Int>> =
        buildSortedMap {
            for ((index, parameter) in function.parameters.withIndex()) {
                getOrPut(parameter.getVersionNumber()) { mutableListOf() }.add(index)
            }
        }

    private fun generateVersions(
        func: IrFunction,
        containingClass: IrClass,
        versionParamIndexes: SortedMap<MavenComparableVersion?, MutableList<Int>>
    ) {
        if (versionParamIndexes.size < 2) return  // just a single version, nothing to do

        val lastIncludedParameters = BooleanArray(func.parameters.size) { true }

        var first = true
        for ((version, params) in versionParamIndexes) {
            if (!first) {
                containingClass.addMember(generateWrapper(func, version, lastIncludedParameters))
            }
            first = false
            lastIncludedParameters.fill(false, params.first(), params.last() + 1)
        }
    }

    private fun IrValueParameter.getVersionNumber(): MavenComparableVersion? {
        if (kind != IrParameterKind.Regular || defaultValue == null) return null
        val annotation = getAnnotation(StandardClassIds.Annotations.IntroducedAt.asSingleFqName()) ?: return null
        val versionString = (annotation.arguments.first() as? IrConst)?.value as? String ?: return null
        return MavenComparableVersion(versionString)
    }

    private fun generateWrapper(original: IrFunction, version: MavenComparableVersion?, includedParams: BooleanArray): IrFunction =
        irFactory.generateWrapperHeader(original, version, includedParams).apply {
            val wrapperCall = generateWrapperCall(original, includedParams)
            body = when (original) {
                is IrConstructor -> irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(wrapperCall))
                else -> irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET) {
                    statements += IrReturnImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, symbol, wrapperCall)
                }
            }
        }

    private fun IrFactory.generateWrapperHeader(
        original: IrFunction,
        version: MavenComparableVersion?,
        includedParams: BooleanArray
    ): IrFunction {
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
            annotations = original.annotations.memoryOptimizedMapNotNull {
                if (it.isAnnotation(StandardClassIds.Annotations.jvmOverloads.asSingleFqName()))
                    null
                else
                    it.transform(DeepCopyIrTreeWithSymbols(SymbolRemapper.EMPTY), null) as IrConstructorCall
            } memoryOptimizedPlus buildDeprecationCall(version)
            copyTypeParametersFrom(original)
            generateNewValueParameters(original, includedParams)
        }
    }

    private fun IrFunction.generateNewValueParameters(original: IrFunction, includedParams: BooleanArray) {
        val originalDefaults = mutableListOf<IrExpressionBody?>()
        parameters = original.parameters.mapIndexedNotNull { i, param ->
            if (!includedParams[i]) null
            else {
                originalDefaults.push(param.defaultValue)
                param.copyTo(this, defaultValue = null)
            }
        }

        // copy the value params first before the default values. required when there are default expressions that depend on other value params

        val transformer = GetValueTransformer(this)
        for ((i, param) in parameters.withIndex()) {
            val originalDefault = originalDefaults[i] ?: continue

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

    private val deprecationLevelHiddenSymbol: IrEnumEntrySymbol by lazy {
        context.irBuiltIns.deprecationLevelSymbol.owner.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.toString() == "HIDDEN" }.symbol
    }

    @OptIn(InternalSymbolFinderAPI::class)
    fun buildDeprecationCall(version: MavenComparableVersion?): IrConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
        SYNTHETIC_OFFSET,
        SYNTHETIC_OFFSET,
        context.irBuiltIns.deprecatedSymbol.defaultType,
        context.irBuiltIns.deprecatedSymbol.constructors.first()
    ).apply {
        arguments[0] =
            IrConstImpl.string(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                context.irBuiltIns.stringType,
                "This method is kept for binary compatibility purposes, please use the main overload. " +
                        "This overload corresponds to ${version?.let { "version $it" } ?: "the initial version"}."
            )
        arguments[2] =
            IrGetEnumValueImpl(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                context.irBuiltIns.deprecationLevelSymbol.defaultType,
                deprecationLevelHiddenSymbol
            )
    }

    fun IrFunction.generateWrapperCall(original: IrFunction, includedParams: BooleanArray): IrFunctionAccessExpression {
        val call = when (original) {
            is IrConstructor -> IrDelegatingConstructorCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, irBuiltIns.unitType, original.symbol
            )
            is IrSimpleFunction -> IrCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, original.returnType, original.symbol
            )
        }

        for (arg in this.allTypeParameters) {
            call.typeArguments[arg.index] = arg.defaultType
        }

        var lastWrapperIndex = 0
        for (originalIndex in original.parameters.indices) {
            if (!includedParams[originalIndex]) {
                call.arguments[originalIndex] = null
            } else {
                val wrapperParam = this.parameters[lastWrapperIndex]
                call.arguments[originalIndex] = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, wrapperParam.symbol)
                lastWrapperIndex += 1
            }
        }
        return call
    }
}

private inline fun <K : Comparable<K>, V> buildSortedMap(block: SortedMap<K?, V>.() -> Unit): SortedMap<K?, V> =
    sortedMapOf<K?, V>(nullsLast(compareByDescending { it })).apply(block)
