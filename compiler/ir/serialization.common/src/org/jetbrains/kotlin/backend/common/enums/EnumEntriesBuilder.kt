/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.enums

import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.IrSyntheticBodyImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class EnumEntriesBuilder(
    private val builtIns: IrBuiltIns,
    private val symbolTable: SymbolTable,
    private val linker: FileLocalAwareLinker,
    mangler: KotlinMangler.IrMangler
) {
    private val candidatesForEnumEntries = mutableSetOf<IrClass>()
    private val signatureComputer = PublicIdSignatureComputer(mangler)

    fun enqueueClass(irClass: IrClass) {
        if (irClass.containsEnumEntriesProperty()) return
        candidatesForEnumEntries.add(irClass)
    }

    private fun IrClass.containsEnumEntriesProperty(): Boolean =
        declarations.any { it is IrProperty && it.getter.hasSyntheticBodyWithKind(IrSyntheticBodyKind.ENUM_ENTRIES) }

    private fun IrSimpleFunction?.hasSyntheticBodyWithKind(kind: IrSyntheticBodyKind): Boolean {
        return (this?.body as? IrSyntheticBody)?.kind == kind
    }

    fun provideEnumEntries() {
        candidatesForEnumEntries.forEach {
            it.provideEnumEntries()
        }
        candidatesForEnumEntries.clear()
    }

    private fun IrClass.provideEnumEntries() {
        val file = fileOrNull ?: return addEnumEntriesProperty()
        signatureComputer.inFile(file.symbol) {
            addEnumEntriesProperty()
        }
    }

    private fun IrClass.addEnumEntriesProperty() {
        declarations.add(generateEnumEntriesProperty().asLinked())
    }

    private fun IrClass.generateEnumEntriesProperty(): IrProperty {
        return symbolTable.irFactory.createProperty(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.PROPERTY_FOR_ENUM_ENTRIES,
            symbol = IrPropertySymbolImpl(),
            name = StandardNames.ENUM_ENTRIES,
            modality = Modality.FINAL,
            visibility = DescriptorVisibilities.PUBLIC,
            isVar = false,
            isConst = false,
            isLateinit = false,
            isDelegated = false,
            isExternal = false,
            isExpect = false,
            isFakeOverride = false,
        ).apply {
            parent = this@generateEnumEntriesProperty
            getter = generateEnumEntriesGetter(symbol)
        }
    }

    private fun IrClass.generateEnumEntriesType(): IrType {
        return builtIns.enumEntriesClass.typeWith(defaultType)
    }

    private fun IrClass.generateEnumEntriesGetter(propertySymbol: IrPropertySymbol): IrSimpleFunction {
        return symbolTable.irFactory.createFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            symbol = IrSimpleFunctionSymbolImpl(),
            name = StandardNames.ENUM_ENTRIES.getterName,
            modality = Modality.FINAL,
            visibility = DescriptorVisibilities.PUBLIC,
            returnType = generateEnumEntriesType(),
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExpect = false,
            isFakeOverride = false,
        ).apply {
            parent = this@generateEnumEntriesGetter
            body = IrSyntheticBodyImpl(startOffset, endOffset, IrSyntheticBodyKind.ENUM_ENTRIES)
            correspondingPropertySymbol = propertySymbol
        }
    }

    private fun IrDeclaration.composeSignature() =
        signatureComputer.computeSignature(this)

    private fun IrProperty.asLinked(): IrProperty {
        val parent = parentAsClass
        val signature = composeSignature()
        val referencedSymbol = linker.tryReferencingPropertyByLocalSignature(parent, signature)
            ?: symbolTable.referenceProperty(signature, false)

        return symbolTable.declareProperty(signature, { referencedSymbol }) {
            assert(it === referencedSymbol)

            if (!it.isBound) {
                it.bind(this)
            }

            referencedSymbol.owner.apply {
                getter = getter?.asLinked()
                getter?.correspondingPropertySymbol = referencedSymbol
                symbol.privateSignature = signature
            }
        }
    }

    private fun IrSimpleFunction.asLinked(): IrSimpleFunction {
        val parent = parentAsClass
        val signature = composeSignature()
        val referencedSymbol = linker.tryReferencingSimpleFunctionByLocalSignature(parent, signature)
            ?: symbolTable.referenceSimpleFunction(signature, false)

        return symbolTable.declareSimpleFunction(signature, { referencedSymbol }) {
            assert(it === referencedSymbol)

            if (!it.isBound) {
                it.bind(this)
            }

            it.owner.apply {
                symbol.privateSignature = signature
            }
        }
    }

    private val Name.getterName: Name
        get() = Name.special("<get-$identifier>")
}

