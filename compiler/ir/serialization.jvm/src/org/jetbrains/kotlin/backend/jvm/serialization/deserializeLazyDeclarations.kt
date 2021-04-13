/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.overrides.DefaultFakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinder
import org.jetbrains.kotlin.backend.common.serialization.IrDeclarationDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFile
import org.jetbrains.kotlin.backend.common.serialization.IrSymbolDeserializer
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.LazyIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.protobuf.ByteString

fun deserializeClassFromByteArray(
    byteArray: ByteArray,
    moduleDescriptor: ModuleDescriptor,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    parent: IrDeclarationParent,
    allowErrorNodes: Boolean,
) {
    val irProto = JvmIr.JvmIrClass.parseFrom(byteArray)
    val irLibraryFile = IrLibraryFileFromAnnotation(
        irProto.auxTables.typeList,
        irProto.auxTables.signatureList,
        irProto.auxTables.stringList,
        irProto.auxTables.bodyList
    )
    val descriptorFinder =
        DescriptorByIdSignatureFinder(moduleDescriptor, JvmDescriptorMangler(null), DescriptorByIdSignatureFinder.LookupMode.MODULE_WITH_DEPENDENCIES)
    val symbolDeserializer = IrSymbolDeserializer(
        symbolTable,
        irLibraryFile,
        /* TODO */ actuals = emptyList(),
        enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
        handleExpectActualMapping = { _, _ -> TODO() },
        deserializePublicSymbol = { idSignature, symbolKind ->
            referencePublicSymbol(symbolTable, descriptorFinder, idSignature, symbolKind)
        }
    )

    val lazyIrFactory = LazyIrFactory(irBuiltIns.irFactory)

    val deserializer = IrDeclarationDeserializer(
        irBuiltIns, symbolTable, lazyIrFactory, irLibraryFile, parent,
        allowErrorNodes = allowErrorNodes,
        deserializeInlineFunctions = true,
        deserializeBodies = true,
        symbolDeserializer,
        DefaultFakeOverrideClassFilter,
        makeEmptyFakeOverrideBuilder(symbolTable, irBuiltIns)
    )

    deserializer.deserializeIrClass(irProto.irClass)
}

fun deserializeIrFileFromByteArray(
    byteArray: ByteArray,
    moduleDescriptor: ModuleDescriptor,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    facadeClass: IrClass,
    allowErrorNodes: Boolean,
) {
    val irProto = JvmIr.JvmIrFile.parseFrom(byteArray)
    val irLibraryFile = IrLibraryFileFromAnnotation(
        irProto.auxTables.typeList,
        irProto.auxTables.signatureList,
        irProto.auxTables.stringList,
        irProto.auxTables.bodyList
    )
    val descriptorFinder =
        DescriptorByIdSignatureFinder(moduleDescriptor, JvmDescriptorMangler(null), DescriptorByIdSignatureFinder.LookupMode.MODULE_WITH_DEPENDENCIES)
    val symbolDeserializer = IrSymbolDeserializer(
        symbolTable,
        irLibraryFile,
        /* TODO */ actuals = emptyList(),
        enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
        handleExpectActualMapping = { _, _ -> TODO() },
        deserializePublicSymbol = { idSignature, symbolKind ->
            referencePublicSymbol(symbolTable, descriptorFinder, idSignature, symbolKind)
        }
    )

    val lazyIrFactory = LazyIrFactory(irBuiltIns.irFactory)

    val deserializer = IrDeclarationDeserializer(
        irBuiltIns, symbolTable, lazyIrFactory, irLibraryFile, facadeClass,
        allowErrorNodes = allowErrorNodes,
        deserializeInlineFunctions = true,
        deserializeBodies = true,
        symbolDeserializer,
        DefaultFakeOverrideClassFilter,
        makeEmptyFakeOverrideBuilder(symbolTable, irBuiltIns)
    )
    for (declarationProto in irProto.declarationList) {
        deserializer.deserializeDeclaration(declarationProto)
    }
}

private class IrLibraryFileFromAnnotation(
    val types: List<ByteString>,
    val signatures: List<ByteString>,
    val strings: List<ByteString>,
    val bodies: List<ByteString>,
) : IrLibraryFile() {
    override fun irDeclaration(index: Int): ByteArray {
        error("This method is never supposed to be called")
    }

    override fun type(index: Int): ByteArray = types[index].toByteArray()
    override fun signature(index: Int): ByteArray = signatures[index].toByteArray()
    override fun string(index: Int): ByteArray = strings[index].toByteArray()
    override fun body(index: Int): ByteArray = bodies[index].toByteArray()
}

private fun referencePublicSymbol(
    symbolTable: SymbolTable,
    descriptorFinder: DescriptorByIdSignatureFinder,
    idSig: IdSignature,
    symbolKind: BinarySymbolData.SymbolKind
): IrSymbol {
    with(symbolTable) {
        val descriptor = descriptorFinder.findDescriptorBySignature(idSig)
        return if (descriptor != null) {
            when (symbolKind) {
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClass(descriptor as ClassDescriptor)
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructor(descriptor as ClassConstructorDescriptor)
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntry(descriptor as ClassDescriptor)
                BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceField(descriptor as PropertyDescriptor)
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunction(descriptor as FunctionDescriptor)
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAlias(descriptor as TypeAliasDescriptor)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referenceProperty(descriptor as PropertyDescriptor)
                else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
            }
        } else {
            when (symbolKind) {
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClassFromLinker(idSig)
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructorFromLinker(idSig)
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntryFromLinker(idSig)
                BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceFieldFromLinker(idSig)
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunctionFromLinker(idSig)
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAliasFromLinker(idSig)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referencePropertyFromLinker(idSig)
                else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
            }
        }
    }
}

// TODO: implement properly
private fun makeEmptyFakeOverrideBuilder(symbolTable: SymbolTable, irBuiltIns: IrBuiltIns): FakeOverrideBuilder {
    return FakeOverrideBuilder(
        object : FileLocalAwareLinker {
            override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? = null
            override fun tryReferencingSimpleFunctionByLocalSignature(
                parent: IrDeclaration, idSignature: IdSignature
            ): IrSimpleFunctionSymbol? = null
        },
        symbolTable,
        IdSignatureSerializer(JvmIrMangler),
        irBuiltIns
    )
}

