/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.guessName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.error.ErrorUtils

/**
 * Generates the simplest possible stubs for missing declarations.
 *
 * Note: This is a special type of [IrProvider]. It should not be used in row with other IR providers, because it may bring to
 * undesired situation when stubs for unbound fake override symbols are generated even before the corresponding call of
 * [FakeOverrideBuilder.provideFakeOverrides] is made leaving no chance for proper linkage of fake overrides. This IR provider
 * should be applied only after the fake overrides generation.
 */
internal class MissingDeclarationStubGenerator(private val builtIns: IrBuiltIns) : IrProvider {
    private val commonParent by lazy {
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(ErrorUtils.errorModule, FqName.ROOT)
    }

    private var declarationsToPatch = arrayListOf<IrDeclaration>()

    fun grabDeclarationsToPatch(): Collection<IrDeclaration> {
        val result = declarationsToPatch
        declarationsToPatch = arrayListOf()
        return result
    }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration {
        require(!symbol.isBound)

        return when (symbol) {
            is IrClassSymbol -> generateClass(symbol)
            is IrSimpleFunctionSymbol -> generateSimpleFunction(symbol)
            is IrConstructorSymbol -> generateConstructor(symbol)
            is IrPropertySymbol -> generateProperty(symbol)
            is IrEnumEntrySymbol -> generateEnumEntry(symbol)
            is IrTypeAliasSymbol -> generateTypeAlias(symbol)
            else -> throw NotImplementedError("Generation of stubs for ${symbol::class.java} is not supported yet")
        }
    }

    private fun generateClass(symbol: IrClassSymbol): IrClass {
        return builtIns.irFactory.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            symbol = symbol,
            name = symbol.guessName(),
            kind = ClassKind.CLASS,
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            modality = Modality.OPEN
        ).apply {
            setCommonParent()
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }
    }

    private fun generateSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunction {
        return builtIns.irFactory.createFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            symbol = symbol,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            modality = Modality.FINAL,
            returnType = builtIns.nothingType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExpect = false
        ).setCommonParent()
    }

    private fun generateConstructor(symbol: IrConstructorSymbol): IrConstructor {
        return builtIns.irFactory.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            symbol = symbol,
            name = SpecialNames.INIT,
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            returnType = builtIns.nothingType,
            isInline = false,
            isExternal = false,
            isPrimary = false,
            isExpect = false,
        ).setCommonParent()
    }

    private fun generateProperty(symbol: IrPropertySymbol): IrProperty {
        return builtIns.irFactory.createProperty(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            symbol = symbol,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            modality = Modality.FINAL,
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
            symbol = symbol,
            name = symbol.guessName()
        ).setCommonParent()
    }

    private fun generateTypeAlias(symbol: IrTypeAliasSymbol): IrTypeAlias {
        return builtIns.irFactory.createTypeAlias(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            symbol = symbol,
            name = symbol.guessName(),
            visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
            expandedType = builtIns.nothingType,
            isActual = true,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
        ).setCommonParent()
    }

    private fun <T : IrDeclaration> T.setCommonParent(): T {
        parent = commonParent
        declarationsToPatch += this
        return this
    }

    private fun IrSymbol.guessName(): Name =
        signature?.guessName(nameSegmentsToPickUp = 1)?.let(Name::guessByFirstCharacter) ?: PartialLinkageUtils.UNKNOWN_NAME
}
