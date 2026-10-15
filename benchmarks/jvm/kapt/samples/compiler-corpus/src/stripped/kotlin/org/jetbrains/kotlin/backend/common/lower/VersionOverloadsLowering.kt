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
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
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

    override fun lower(irFile: IrFile) { TODO() }

    override fun visitFunction(declaration: IrFunction): IrStatement = TODO()

    fun generateVersionOverloads(target: IrFunction) { TODO() }

    private fun getVersionParameterRawIndexes(function: IrFunction): Map<String?, MutableList<Int>> = TODO()

    private fun IrFunction.generateVersions(
        container: IrDeclarationContainer,
        versionParamRawIndexes: Map<String?, MutableList<Int>>
    ) = irFactory.stageController.restrictTo(this) {
        val versionParamIndexes = buildSortedMap {
            for ([version, indexes] in versionParamRawIndexes) {
                getOrPut(version?.let(::MavenComparableVersion)) { mutableListOf() }.addAll(indexes)
            }
        }

        val lastIncludedParameters = BooleanArray(parameters.size) { true }
        var first = true
        for ([version, params] in versionParamIndexes) {
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

    protected open fun generateWrapper(original: IrFunction, version: MavenComparableVersion?, includedParams: BooleanArray): IrFunction = TODO()

    protected open fun generateWrapperHeader(
        original: IrFunction,
        version: MavenComparableVersion?,
        includedParams: BooleanArray
    ): IrFunction = TODO()

    private fun IrFunction.generateNewValueParameters(
        original: IrFunction,
        includedParams: BooleanArray,
        typeParameterSubstitution: Map<IrTypeParameter, IrTypeParameter>
    ) { TODO() }

    private object ParameterRemapper : IrTransformer<Map<IrValueParameterSymbol, IrValueParameter>>() {
        override fun visitGetValue(expression: IrGetValue, data: Map<IrValueParameterSymbol, IrValueParameter>): IrExpression { TODO() }
    }

    private val deprecationLevelErrorSymbol: IrEnumEntrySymbol by lazy {
        irBuiltIns.deprecationLevelSymbol.owner.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.toString() == "ERROR" }.symbol
    }

    fun buildDeprecationCall(version: MavenComparableVersion?): IrAnnotation = TODO()

    protected open fun IrFunction.generateWrapperCall(original: IrFunction, includedParams: BooleanArray): IrFunctionAccessExpression { TODO() }
}

private inline fun <K : Comparable<K>, V> buildSortedMap(block: SortedMap<K?, V>.() -> Unit): SortedMap<K?, V> =
    sortedMapOf<K?, V>(nullsLast(compareByDescending { it })).apply(block)
