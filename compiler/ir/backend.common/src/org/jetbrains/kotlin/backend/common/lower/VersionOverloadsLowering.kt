/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import java.util.*

open class VersionOverloadsLowering(val irFactory: IrFactory, val irBuiltIns: IrBuiltIns) : FileLoweringPass, IrElementTransformerVoid() {
    constructor(context: LoweringContext) : this(context.irFactory, context.irBuiltIns)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitFunction(declaration: IrFunction): IrStatement = declaration.also {
        generateVersionOverloads(it)
    }

    fun generateVersionOverloads(target: IrFunction) {
        val irParent: IrDeclarationContainer = target.parent as? IrDeclarationContainer ?: return

        val targetIsCopy =
            target is IrSimpleFunction && target.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER
                    && target.name == StandardNames.DATA_CLASS_COPY
                    && irParent is IrClass && irParent.isData

        val versionParamRawIndexes = if (targetIsCopy) {
            // adjust the information from the primary constructor into that of 'copy'
            getVersionParameterRawIndexes(irParent.primaryConstructor!!).also {
                it.forEach { (_, params) -> params.indices.forEach { ix -> params[ix] += 1 } }
                it[null]?.add(0)
            }
        } else getVersionParameterRawIndexes(target)

        if (versionParamRawIndexes.size <= 1) return // nothing to do

        target.generateVersions(irParent, versionParamRawIndexes)
    }

    private fun getVersionParameterRawIndexes(function: IrFunction): Map<String?, MutableList<Int>> =
        buildMap {
            put(null, mutableListOf()) // we always have the 'no annotation' case
            for ((index, parameter) in function.parameters.withIndex()) {
                val version = when {
                    parameter.kind != IrParameterKind.Regular -> null
                    parameter.defaultValue == null -> null
                    else -> {
                        val annotation = parameter.getAnnotation(StandardNames.FqNames.introducedAt)
                        (annotation?.arguments?.first() as? IrConst)?.value as? String
                    }
                }
                getOrPut(version) { mutableListOf() }.add(index)
            }
        }

    private fun IrFunction.generateVersions(
        container: IrDeclarationContainer,
        versionParamRawIndexes: Map<String?, MutableList<Int>>
    ) = irFactory.stageController.restrictTo(this) {
        val versionParamIndexes = buildSortedMap {
            for ((version, indexes) in versionParamRawIndexes) {
                getOrPut(version?.let(::MavenComparableVersion)) { mutableListOf() }.addAll(indexes)
            }
        }

        val lastIncludedParameters = BooleanArray(parameters.size) { true }
        var first = true
        for ((version, params) in versionParamIndexes) {
            if (!first) {
                val newWrapper = generateWrapper(this, version, lastIncludedParameters)
                container.declarations.add(newWrapper)
            } else {
                first = false
            }
            for (paramIndex in params) {
                lastIncludedParameters[paramIndex] = false
            }
        }
    }

    protected open fun generateWrapper(original: IrFunction, version: MavenComparableVersion?, includedParams: BooleanArray): IrFunction =
        generateWrapperHeader(original, version, includedParams).apply {
            val wrapperCall = generateWrapperCall(original, includedParams)
            body = when (original) {
                is IrConstructor -> irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(wrapperCall))
                else -> irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET) {
                    statements += IrReturnImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, symbol, wrapperCall)
                }
            }
        }

    protected open fun generateWrapperHeader(
        original: IrFunction,
        version: MavenComparableVersion?,
        includedParams: BooleanArray
    ): IrFunction = with(irFactory) {
        val builder = when (original) {
            is IrConstructor -> ::buildConstructor
            else -> ::buildFun
        }
        return builder {
            updateFrom(original)
            name = original.name
            origin = IrDeclarationOrigin.VERSION_OVERLOAD_WRAPPER
            if (original is IrConstructor) isPrimary = false
        }.apply {
            with(irFactory) { declarationCreated() }
            parent = original.parent
            copyAnnotationsFrom(original)
            annotations = annotations memoryOptimizedPlus buildDeprecationCall(version)
            val newParameters = copyTypeParametersFrom(original)
            val typeParameterSubstitution = original.typeParameters.zip(newParameters).toMap()
            returnType = original.returnType.remapTypeParameters(original, this, typeParameterSubstitution)
            generateNewValueParameters(original, includedParams, typeParameterSubstitution)
        }
    }

    private fun IrFunction.generateNewValueParameters(
        original: IrFunction,
        includedParams: BooleanArray,
        typeParameterSubstitution: Map<IrTypeParameter, IrTypeParameter>
    ) {
        val oldToNewParameterMap = mutableMapOf<IrValueParameterSymbol, IrValueParameter>()

        for ((i, originalParam) in original.parameters.withIndex()) {
            if (!includedParams[i]) continue

            val newParam = originalParam.copyTo(this, defaultValue = null, remapTypeMap = typeParameterSubstitution)

            val originalDefault = originalParam.defaultValue
            if (originalDefault != null) {
                newParam.defaultValue = factory.createExpressionBody(
                    startOffset = originalDefault.startOffset,
                    endOffset = originalDefault.endOffset,
                    expression = originalDefault.expression.deepCopyWithSymbols(
                        initialParent = this,
                        createTypeRemapper = { IrTypeParameterRemapper(typeParameterSubstitution) }
                    ).transform(ParameterRemapper, oldToNewParameterMap),
                )
            }

            parameters = parameters + newParam
            oldToNewParameterMap[originalParam.symbol] = newParam
        }
    }

    private object ParameterRemapper : IrTransformer<Map<IrValueParameterSymbol, IrValueParameter>>() {
        override fun visitGetValue(expression: IrGetValue, data: Map<IrValueParameterSymbol, IrValueParameter>): IrExpression {
            val newSymbol = data[expression.symbol] ?: return expression
            return IrGetValueImpl(expression.startOffset, expression.endOffset, newSymbol.type, newSymbol.symbol, expression.origin)
        }
    }

    private val deprecationLevelHiddenSymbol: IrEnumEntrySymbol by lazy {
        irBuiltIns.deprecationLevelSymbol.owner.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.toString() == "HIDDEN" }.symbol
    }

    fun buildDeprecationCall(version: MavenComparableVersion?): IrConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
        SYNTHETIC_OFFSET,
        SYNTHETIC_OFFSET,
        irBuiltIns.deprecatedSymbol.defaultType,
        irBuiltIns.deprecatedSymbol.constructors.first()
    ).apply {
        arguments[0] =
            IrConstImpl.string(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                irBuiltIns.stringType,
                "This method is kept for binary compatibility purposes, please use the main overload. " +
                        "This overload corresponds to ${version?.let { "version $it" } ?: "the initial version"}."
            )
        arguments[2] =
            IrGetEnumValueImpl(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                irBuiltIns.deprecationLevelSymbol.defaultType,
                deprecationLevelHiddenSymbol
            )
    }

    protected open fun IrFunction.generateWrapperCall(original: IrFunction, includedParams: BooleanArray): IrFunctionAccessExpression {
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
