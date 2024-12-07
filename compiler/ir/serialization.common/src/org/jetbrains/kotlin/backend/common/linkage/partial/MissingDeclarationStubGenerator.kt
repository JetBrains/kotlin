/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageUtils.guessName
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.linkage.partial.PartiallyLinkedDeclarationOrigin
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.error.ErrorUtils

/**
 * Generates the simplest possible stubs for missing declarations.
 *
 * Note: This is a special type of stub generator. It should not be used in row with [IrProvider]s, because it may bring to
 * an undesired situation when stubs for unbound fake override symbols are generated even before the corresponding call of
 * [IrLinkerFakeOverrideProvider.provideFakeOverrides] is made leaving no chance for proper linkage of fake overrides.
 * This stub generator should be applied only after the fake overrides generation.
 */
internal class MissingDeclarationStubGenerator(private val builtIns: IrBuiltIns) {
    private val commonParent by lazy {
        createEmptyExternalPackageFragment(ErrorUtils.errorModule, FqName.ROOT)
    }

    private var declarationsToPatch = arrayListOf<IrDeclaration>()

    private val stubbedSymbols = hashSetOf<IrSymbol>()

    val allStubbedSymbols: Set<IrSymbol> get() = stubbedSymbols

    fun grabDeclarationsToPatch(): Collection<IrDeclaration> {
        val result = declarationsToPatch
        declarationsToPatch = arrayListOf()
        return result
    }

    fun getDeclaration(symbol: IrSymbol): IrDeclaration {
        require(!symbol.isBound)

        stubbedSymbols.add(symbol)

        return when (symbol) {
            is IrClassSymbol -> generateClass(symbol)
            is IrSimpleFunctionSymbol -> generateSimpleFunction(symbol)
            is IrConstructorSymbol -> generateConstructor(symbol)
            is IrPropertySymbol -> generateProperty(symbol)
            is IrEnumEntrySymbol -> generateEnumEntry(symbol)
            is IrTypeAliasSymbol -> generateTypeAlias(symbol)
            is IrTypeParameterSymbol -> generateTypeParameter(symbol)
            else -> throw NotImplementedError("Generation of stubs for ${symbol::class.java} is not supported yet")
        }
    }

    private fun generateClass(symbol: IrClassSymbol): IrClass {
        return builtIns.irFactory.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            symbol = symbol,
            kind = ClassKind.CLASS,
            modality = Modality.OPEN,
        ).apply {
            setCommonParent()
            createThisReceiverParameter()
        }
    }

    private fun generateSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunction {
        return builtIns.irFactory.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            isInline = false,
            isExpect = false,
            returnType = builtIns.nothingType,
            modality = Modality.FINAL,
            symbol = symbol,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false
        ).setCommonParent()
    }

    private fun generateConstructor(symbol: IrConstructorSymbol): IrConstructor {
        return builtIns.irFactory.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = SpecialNames.INIT,
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            isInline = false,
            isExpect = false,
            returnType = builtIns.nothingType,
            symbol = symbol,
            isPrimary = false,
            isExternal = false,
        ).setCommonParent()
    }

    private fun generateProperty(symbol: IrPropertySymbol): IrProperty {
        return builtIns.irFactory.createProperty(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            modality = Modality.FINAL,
            symbol = symbol,
            isVar = false,
            isConst = false,
            isLateinit = false,
            isDelegated = false,
            isExternal = false,
            isExpect = false
        ).setCommonParent()
    }

    private fun generateEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntry {
        return builtIns.irFactory.createEnumEntry(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = symbol.guessName(),
            symbol = symbol,
        ).setCommonParent()
    }

    private fun generateTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAlias {
        return builtIns.irFactory.createTypeAlias(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            symbol = symbol,
            isActual = true,
            expandedType = builtIns.nothingType,
        ).setCommonParent()
    }

    private fun generateTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameter {
        return builtIns.irFactory.createTypeParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            name = symbol.guessName(),
            symbol = symbol,
            variance = Variance.INVARIANT,
            index = 0,
            isReified = false,
        )
    }

    private fun <T : IrDeclaration> T.setCommonParent(): T {
        parent = commonParent
        declarationsToPatch += this
        return this
    }

    private fun IrSymbol.guessName(): Name =
        signature?.guessName(nameSegmentsToPickUp = 1)?.let(Name::guessByFirstCharacter) ?: PartialLinkageUtils.UNKNOWN_NAME
}
