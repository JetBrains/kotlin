/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.persistent.PersistentIrBlockBody
import org.jetbrains.kotlin.ir.expressions.persistent.PersistentIrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrPublicSymbolBase
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.Variance

class PersistentIrFactory : IrFactory {

    override var stageController = StageController()

    val allDeclarations = mutableListOf<IrDeclaration>()

    val allBodies = mutableListOf<IrBody>()

    private fun IrDeclaration.register() {
        allDeclarations += this
    }

    fun declarationSignature(declaration: IrDeclaration): IdSignature? {
        return (declaration as? PersistentIrDeclarationBase<*>)?.signature ?: declaration.symbol.signature
    }

    @Suppress("UNUSED_PARAMETER")
    internal fun currentSignature(declaration: IrDeclaration): IdSignature? {
        val parentSig = stageController.currentDeclaration?.let { declarationSignature(it) } ?: return null

        return stageController.createSignature(parentSig)
    }

    override fun createAnonymousInitializer(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrAnonymousInitializerSymbol,
        isStatic: Boolean,
    ): IrAnonymousInitializer =
        PersistentIrAnonymousInitializer(startOffset, endOffset, origin, symbol, isStatic, this).also { it.register() }

    override fun createClass(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrClassSymbol,
        name: Name,
        kind: ClassKind,
        visibility: DescriptorVisibility,
        modality: Modality,
        isCompanion: Boolean,
        isInner: Boolean,
        isData: Boolean,
        isExternal: Boolean,
        isInline: Boolean,
        isExpect: Boolean,
        isFun: Boolean,
        source: SourceElement,
    ): IrClass =
        PersistentIrClass(
            startOffset, endOffset, origin, symbol, name, kind, visibility, modality,
            isCompanion, isInner, isData, isExternal, isInline, isExpect, isFun, source,
            this
        ).also { it.register() }

    override fun createConstructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrConstructorSymbol,
        name: Name,
        visibility: DescriptorVisibility,
        returnType: IrType,
        isInline: Boolean,
        isExternal: Boolean,
        isPrimary: Boolean,
        isExpect: Boolean,
        containerSource: DeserializedContainerSource?,
    ): IrConstructor =
        PersistentIrConstructor(
            startOffset, endOffset, origin, symbol, name, visibility, returnType, isInline, isExternal, isPrimary, isExpect,
            containerSource, this
        ).also { it.register() }

    override fun createEnumEntry(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrEnumEntrySymbol,
        name: Name,
    ): IrEnumEntry =
        PersistentIrEnumEntry(startOffset, endOffset, origin, symbol, name, this).also { it.register() }

    override fun createErrorDeclaration(
        startOffset: Int,
        endOffset: Int,
        descriptor: DeclarationDescriptor?,
    ): IrErrorDeclaration =
        PersistentIrErrorDeclaration(startOffset, endOffset, descriptor, this).also { it.register() }

    override fun createField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrFieldSymbol,
        name: Name,
        type: IrType,
        visibility: DescriptorVisibility,
        isFinal: Boolean,
        isExternal: Boolean,
        isStatic: Boolean,
    ): IrField =
        PersistentIrField(startOffset, endOffset, origin, symbol, name, type, visibility, isFinal, isExternal, isStatic, this).also { it.register() }

    override fun createFunction(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrSimpleFunctionSymbol,
        name: Name,
        visibility: DescriptorVisibility,
        modality: Modality,
        returnType: IrType,
        isInline: Boolean,
        isExternal: Boolean,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isExpect: Boolean,
        isFakeOverride: Boolean,
        containerSource: DeserializedContainerSource?,
    ): IrSimpleFunction =
        PersistentIrFunction(
            startOffset, endOffset, origin, symbol, name, visibility, modality, returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, isFakeOverride,
            containerSource, this
        ).also { it.register() }

    override fun createFakeOverrideFunction(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        modality: Modality,
        returnType: IrType,
        isInline: Boolean,
        isExternal: Boolean,
        isTailrec: Boolean,
        isSuspend: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isExpect: Boolean,
    ): IrSimpleFunction =
        PersistentIrFakeOverrideFunction(
            startOffset, endOffset, origin, name, visibility, modality, returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, this
        ).also { it.register() }

    override fun createLocalDelegatedProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrLocalDelegatedPropertySymbol,
        name: Name,
        type: IrType,
        isVar: Boolean,
    ): IrLocalDelegatedProperty =
        PersistentIrLocalDelegatedProperty(
            startOffset, endOffset, origin, symbol, name, type, isVar, this
        ).also { it.register() }

    override fun createProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrPropertySymbol,
        name: Name,
        visibility: DescriptorVisibility,
        modality: Modality,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExternal: Boolean,
        isExpect: Boolean,
        isFakeOverride: Boolean,
        containerSource: DeserializedContainerSource?,
    ): IrProperty =
        PersistentIrProperty(
            startOffset, endOffset, origin, symbol, name, visibility, modality,
            isVar, isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride,
            containerSource, this
        ).also { it.register() }

    override fun createFakeOverrideProperty(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        name: Name,
        visibility: DescriptorVisibility,
        modality: Modality,
        isVar: Boolean,
        isConst: Boolean,
        isLateinit: Boolean,
        isDelegated: Boolean,
        isExternal: Boolean,
        isExpect: Boolean,
    ): IrProperty =
        PersistentIrFakeOverrideProperty(
            startOffset, endOffset, origin, name, visibility, modality,
            isVar, isConst, isLateinit, isDelegated, isExternal, isExpect,
            this
        ).also { it.register() }

    override fun createTypeAlias(
        startOffset: Int,
        endOffset: Int,
        symbol: IrTypeAliasSymbol,
        name: Name,
        visibility: DescriptorVisibility,
        expandedType: IrType,
        isActual: Boolean,
        origin: IrDeclarationOrigin,
    ): IrTypeAlias =
        PersistentIrTypeAlias(startOffset, endOffset, symbol, name, visibility, expandedType, isActual, origin, this).also { it.register() }

    override fun createTypeParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrTypeParameterSymbol,
        name: Name,
        index: Int,
        isReified: Boolean,
        variance: Variance,
    ): IrTypeParameter =
        PersistentIrTypeParameter(startOffset, endOffset, origin, symbol, name, index, isReified, variance, this).also { it.register() }

    override fun createValueParameter(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrValueParameterSymbol,
        name: Name,
        index: Int,
        type: IrType,
        varargElementType: IrType?,
        isCrossinline: Boolean,
        isNoinline: Boolean,
        isHidden: Boolean,
        isAssignable: Boolean
    ): IrValueParameter =
        PersistentIrValueParameter(
            startOffset, endOffset, origin, symbol, name, index, type, varargElementType, isCrossinline, isNoinline, isHidden, isAssignable, this
        ).also { it.register() }

    override fun createExpressionBody(
        startOffset: Int,
        endOffset: Int,
        initializer: IrExpressionBody.() -> Unit,
    ): IrExpressionBody =
        PersistentIrExpressionBody(startOffset, endOffset, this, initializer).also {
            allBodies += it
        }

    override fun createExpressionBody(
        startOffset: Int,
        endOffset: Int,
        expression: IrExpression,
    ): IrExpressionBody =
        PersistentIrExpressionBody(startOffset, endOffset, expression, this).also {
            allBodies += it
        }

    override fun createExpressionBody(
        expression: IrExpression,
    ): IrExpressionBody =
        PersistentIrExpressionBody(expression, this).also {
            allBodies += it
        }

    override fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
    ): IrBlockBody =
        PersistentIrBlockBody(startOffset, endOffset, this).also {
            allBodies += it
        }

    override fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
        statements: List<IrStatement>,
    ): IrBlockBody =
        PersistentIrBlockBody(startOffset, endOffset, statements, this).also {
            allBodies += it
        }

    override fun createBlockBody(
        startOffset: Int,
        endOffset: Int,
        initializer: IrBlockBody.() -> Unit,
    ): IrBlockBody =
        PersistentIrBlockBody(startOffset, endOffset, this, initializer).also {
            allBodies += it
        }
}
