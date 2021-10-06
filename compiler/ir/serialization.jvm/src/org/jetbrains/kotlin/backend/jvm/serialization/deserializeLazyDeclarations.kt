/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.overrides.DefaultFakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideDeclarationTable
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.lazy.LazyIrFactory
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.protobuf.ByteString

fun deserializeClassFromByteArray(
    byteArray: ByteArray,
    stubGenerator: DeclarationStubGenerator,
    irClass: IrClass,
    typeSystemContext: IrTypeSystemContext,
    allowErrorNodes: Boolean,
) {
    val irBuiltIns = stubGenerator.irBuiltIns
    val symbolTable = stubGenerator.symbolTable
    val irProto = JvmIr.JvmIrClass.parseFrom(byteArray)
    val irLibraryFile = IrLibraryFileFromAnnotation(
        irProto.auxTables.typeList,
        irProto.auxTables.signatureList,
        irProto.auxTables.stringList,
        irProto.auxTables.bodyList,
        irProto.auxTables.debugInfoList
    )
    val descriptorFinder =
        DescriptorByIdSignatureFinder(
            stubGenerator.moduleDescriptor,
            JvmDescriptorMangler(null),
            DescriptorByIdSignatureFinder.LookupMode.MODULE_WITH_DEPENDENCIES
        )

    // Only needed for local signature computation.
    val dummyIrFile = IrFileImpl(NaiveSourceBasedFileEntryImpl("<unknown>"), IrFileSymbolImpl(), irClass.packageFqName!!)

    val symbolDeserializer = IrSymbolDeserializer(
        symbolTable,
        irLibraryFile,
        fileSymbol = dummyIrFile.symbol,
        /* TODO */ actuals = emptyList(),
        enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
        handleExpectActualMapping = { _, _ -> TODO() },
        deserializePublicSymbol = { idSignature, symbolKind ->
            referencePublicSymbol(symbolTable, descriptorFinder, idSignature, symbolKind)
        }
    )

    val lazyIrFactory = LazyIrFactory(irBuiltIns.irFactory)

    val deserializer = IrDeclarationDeserializer(
        irBuiltIns, symbolTable, lazyIrFactory, irLibraryFile, irClass.parent,
        allowErrorNodes = allowErrorNodes,
        deserializeInlineFunctions = true,
        deserializeBodies = true,
        symbolDeserializer,
        DefaultFakeOverrideClassFilter,
        makeSimpleFakeOverrideBuilder(symbolTable, typeSystemContext, symbolDeserializer),
        compatibilityMode = CompatibilityMode.CURRENT,
    )

    deserializer.deserializeIrClass(irProto.irClass)

    ExternalDependenciesGenerator(stubGenerator.symbolTable, listOf(stubGenerator)).generateUnboundSymbolsAsDependencies()
    buildFakeOverridesForLocalClasses(stubGenerator.symbolTable, typeSystemContext, symbolDeserializer, irClass)
}

fun deserializeIrFileFromByteArray(
    byteArray: ByteArray,
    stubGenerator: DeclarationStubGenerator,
    facadeClass: IrClass,
    typeSystemContext: IrTypeSystemContext,
    allowErrorNodes: Boolean,
) {
    val irBuiltIns = stubGenerator.irBuiltIns
    val symbolTable = stubGenerator.symbolTable
    val irProto = JvmIr.JvmIrFile.parseFrom(byteArray)
    val irLibraryFile = IrLibraryFileFromAnnotation(
        irProto.auxTables.typeList,
        irProto.auxTables.signatureList,
        irProto.auxTables.stringList,
        irProto.auxTables.bodyList,
        irProto.auxTables.debugInfoList
    )
    val descriptorFinder =
        DescriptorByIdSignatureFinder(
            stubGenerator.moduleDescriptor,
            JvmDescriptorMangler(null),
            DescriptorByIdSignatureFinder.LookupMode.MODULE_WITH_DEPENDENCIES
        )

    // Only needed for local signature computation.
    val dummyIrFile = IrFileImpl(NaiveSourceBasedFileEntryImpl("<unknown>"), IrFileSymbolImpl(), facadeClass.packageFqName!!)

    val symbolDeserializer = IrSymbolDeserializer(
        symbolTable,
        irLibraryFile,
        fileSymbol = dummyIrFile.symbol,
        /* TODO */ actuals = emptyList(),
        enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
        handleExpectActualMapping = { _, _ -> TODO() },
        deserializePublicSymbol = { idSignature, symbolKind ->
            referencePublicSymbol(symbolTable, descriptorFinder, idSignature, symbolKind)
        }
    )

    val lazyIrFactory = LazyIrFactory(irBuiltIns.irFactory)

    val fakeOverrideBuilder = makeSimpleFakeOverrideBuilder(symbolTable, typeSystemContext, symbolDeserializer)

    val deserializer = IrDeclarationDeserializer(
        irBuiltIns, symbolTable, lazyIrFactory, irLibraryFile, facadeClass,
        allowErrorNodes = allowErrorNodes,
        deserializeInlineFunctions = true,
        deserializeBodies = true,
        symbolDeserializer,
        DefaultFakeOverrideClassFilter,
        fakeOverrideBuilder,
        compatibilityMode = CompatibilityMode.CURRENT,
    )
    for (declarationProto in irProto.declarationList) {
        deserializer.deserializeDeclaration(declarationProto)
    }

    ExternalDependenciesGenerator(stubGenerator.symbolTable, listOf(stubGenerator)).generateUnboundSymbolsAsDependencies()
    buildFakeOverridesForLocalClasses(stubGenerator.symbolTable, typeSystemContext, symbolDeserializer, facadeClass)
}

private class IrLibraryFileFromAnnotation(
    private val types: List<ByteString>,
    private val signatures: List<ByteString>,
    private val strings: List<ByteString>,
    private val bodies: List<ByteString>,
    private val debugInfo: List<ByteString>
) : IrLibraryFile() {
    override fun irDeclaration(index: Int): ByteArray {
        error("This method is never supposed to be called")
    }

    override fun type(index: Int): ByteArray = types[index].toByteArray()
    override fun signature(index: Int): ByteArray = signatures[index].toByteArray()
    override fun string(index: Int): ByteArray = strings[index].toByteArray()
    override fun body(index: Int): ByteArray = bodies[index].toByteArray()
    override fun debugInfo(index: Int): ByteArray = debugInfo[index].toByteArray()
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
                BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL, BinarySymbolData.SymbolKind.FIELD_SYMBOL
                    -> referenceField(descriptor as PropertyDescriptor)
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunction(descriptor as FunctionDescriptor)
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAlias(descriptor as TypeAliasDescriptor)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referenceProperty(descriptor as PropertyDescriptor)
                BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameter(descriptor as TypeParameterDescriptor)
                else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
            }
        } else {
            when (symbolKind) {
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClassFromLinker(idSig)
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructorFromLinker(idSig)
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntryFromLinker(idSig)
                BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL, BinarySymbolData.SymbolKind.FIELD_SYMBOL
                    -> referenceFieldFromLinker(idSig)
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunctionFromLinker(idSig)
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAliasFromLinker(idSig)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referencePropertyFromLinker(idSig)
                else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
            }
        }
    }
}

// TODO: implement properly
fun makeSimpleFakeOverrideBuilder(
    symbolTable: SymbolTable,
    typeSystemContext: IrTypeSystemContext,
    symbolDeserializer: IrSymbolDeserializer
): FakeOverrideBuilder {
    return FakeOverrideBuilder(
        object : FileLocalAwareLinker {
            override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol =
                symbolDeserializer.referencePropertyByLocalSignature(idSignature)

            override fun tryReferencingSimpleFunctionByLocalSignature(
                parent: IrDeclaration, idSignature: IdSignature
            ): IrSimpleFunctionSymbol =
                symbolDeserializer.referenceSimpleFunctionByLocalSignature(idSignature)
        },
        symbolTable,
        JvmIrMangler,
        typeSystemContext,
        fakeOverrideDeclarationTable = PrePopulatedDeclarationTable(symbolDeserializer.deserializedSymbols),
        friendModules = emptyMap() // TODO: provide friend modules
    )
}

private fun buildFakeOverridesForLocalClasses(
    symbolTable: SymbolTable,
    typeSystemContext: IrTypeSystemContext,
    symbolDeserializer: IrSymbolDeserializer,
    toplevel: IrClass
) {
    val builder = makeSimpleFakeOverrideBuilder(symbolTable, typeSystemContext, symbolDeserializer)
    toplevel.acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            if (declaration.visibility == DescriptorVisibilities.LOCAL) {
                builder.provideFakeOverrides(declaration, CompatibilityMode.CURRENT)
            }
            super.visitClass(declaration)
        }
    })
}

class PrePopulatedDeclarationTable(
    sig2symbol: Map<IdSignature, IrSymbol>
) : FakeOverrideDeclarationTable(JvmIrMangler, signatureSerializerFactory = ::IdSignatureSerializer) {
    private val symbol2Sig = sig2symbol.entries.associate { (x, y) -> y to x }

    override fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? {
        symbol2Sig[declaration.symbol]?.let { return it }
        return super.tryComputeBackendSpecificSignature(declaration)
    }
}