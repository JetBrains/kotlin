/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

// TODO: use some class to bear information about the inline function where the accessor is needed
typealias InlineFunctionInfo = Nothing?

class KlibSyntheticAccessorGenerator(
    context: LoweringContext
) : SyntheticAccessorGenerator<LoweringContext, InlineFunctionInfo>(context) {

    private data class OuterThisAccessorKey(val innerClass: IrClass)

    companion object {
        private const val TOP_LEVEL_DECLARATION_SUFFIX_MARKER = "t"

        private var IrValueParameter.outerThisSyntheticAccessors: MutableMap<OuterThisAccessorKey, IrSimpleFunction>? by irAttribute(
            copyByDefault = false
        )
    }

    override fun IrConstructor.makeConstructorAccessor(originForConstructorAccessor: IrDeclarationOrigin): IrFunction { TODO() }

    override fun accessorModality(parent: IrDeclarationParent) = Modality.FINAL
    override fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopeInfo: InlineFunctionInfo) = parent

    override fun AccessorNameBuilder.buildFunctionName(
        function: IrSimpleFunction,
        superQualifier: IrClassSymbol?,
        scopeInfo: InlineFunctionInfo,
    ) { TODO() }

    override fun AccessorNameBuilder.buildFieldGetterName(field: IrField, superQualifierSymbol: IrClassSymbol?) { TODO() }

    override fun AccessorNameBuilder.buildFieldSetterName(field: IrField, superQualifierSymbol: IrClassSymbol?) { TODO() }

    override fun capturedTypeParametersOfSyntheticAccessor(declaration: IrDeclaration): List<IrTypeParameter> { TODO() }

    override fun createAccessorMarkerArgument(): IrConst = TODO()

    private fun AccessorNameBuilder.contributeTopLevelDeclarationSuffix(declaration: IrDeclaration) { TODO() }

    /**
     * This is a special kind of _private_ non-static accessor specifically for accessing "outer this"
     * implicit value parameter from within the scope of the class instance. This accessor is not intended
     * to be a part of ABI. The accessor is created by [OuterThisInInlineFunctionsSpecialAccessorLowering].
     *
     * Note: A new non-private static accessor may be generated later in [SyntheticAccessorLowering] as
     * a wrapper for _this_ accessor allowing calling it inside inline functions (and as a result getting "outer this").
     * The new static accessor may become a part of public ABI if it has _public_ visibility.
     */
    fun getSyntheticOuterThisParameterAccessor(
        expression: IrGetValue,
        outerThisValueParameter: IrValueParameter,
        innerClass: IrClass
    ): IrSimpleFunction { TODO() }

    private fun makeSyntheticOuterThisParameterAccessor(
        expression: IrGetValue,
        innerClass: IrClass,
        outerClass: IrClass
    ): IrSimpleFunction { TODO() }
}
