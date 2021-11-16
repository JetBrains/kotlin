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
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.LazyIrFactory
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType

fun deserializeFromByteArray(
    byteArray: ByteArray,
    stubGenerator: DeclarationStubGenerator,
    toplevelParent: IrClass,
    typeSystemContext: IrTypeSystemContext,
    allowErrorNodes: Boolean,
) {
    val irBuiltIns = stubGenerator.irBuiltIns
    val symbolTable = stubGenerator.symbolTable
    val irProto = JvmIr.ClassOrFile.parseFrom(byteArray.codedInputStream)
    val irLibraryFile = IrLibraryFileFromAnnotation(
        irProto.typeList,
        irProto.signatureList,
        irProto.stringList,
        irProto.bodyList,
        irProto.debugInfoList
    )
    val descriptorFinder =
        DescriptorByIdSignatureFinderImpl(
            stubGenerator.moduleDescriptor,
            JvmDescriptorMangler(null),
            DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_WITH_DEPENDENCIES
        )

    // Only needed for local signature computation.
    val dummyIrFile = IrFileImpl(NaiveSourceBasedFileEntryImpl("<unknown>"), IrFileSymbolImpl(), toplevelParent.packageFqName!!)
    val dummyFileSignature = IdSignature.FileSignature(Any(), toplevelParent.packageFqName!!, "<unknown>")

    val symbolDeserializer = IrSymbolDeserializer(
        symbolTable,
        irLibraryFile,
        fileSymbol = dummyIrFile.symbol,
        fileSignature = dummyFileSignature,
        /* TODO */ actuals = emptyList(),
        enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
        handleExpectActualMapping = { _, symbol -> symbol } no expect declarations
    ) { idSignature, symbolKind ->
        referencePublicSymbol(symbolTable, descriptorFinder, idSignature, symbolKind)
    }

    val lazyIrFactory = LazyIrFactory(irBuiltIns.irFactory)

    val fakeOverrideBuilder = makeSimpleFakeOverrideBuilder(symbolTable, typeSystemContext, symbolDeserializer)

    // We have to supply topLevelParent here, but this results in wrong values for parent fields in deeply embedded declarations.
    // Patching will be needed.
    val deserializer = IrDeclarationDeserializer(
        irBuiltIns, symbolTable, lazyIrFactory, irLibraryFile, toplevelParent,
        allowErrorNodes = allowErrorNodes,
        deserializeInlineFunctions = true,
        deserializeBodies = true,
        symbolDeserializer,
        DefaultFakeOverrideClassFilter,
        fakeOverrideBuilder,
        compatibilityMode = CompatibilityMode.CURRENT,
    )
    for (declarationProto in irProto.declarationList) {
        val declaration = deserializer.deserializeDeclaration(declarationProto)
        // Either declaration is lazy, supplied by LazyIrFactory,
        // or, if it is newly created, there must have been no references to it from the current module.
        // These newly created declarations will never be used, so it does not matter if we patch their parent or not.
        if (declaration is IrLazyDeclarationBase) {
            declaration.parent = declaration.lazyParent()
        }
    }

    stubGenerator.symbolTable.signaturer.withFileSignature(dummyFileSignature) {
        ExternalDependenciesGenerator(stubGenerator.symbolTable, listOf(stubGenerator)).generateUnboundSymbolsAsDependencies()
    }
    buildFakeOverridesForLocalClasses(stubGenerator.symbolTable, typeSystemContext, symbolDeserializer, toplevelParent)
}

private class IrLibraryFileFromAnnotation(
    private val types: List<ProtoType>,
    private val signatures: List<ProtoIdSignature>,
    private val strings: List<String>,
    private val bodies: List<JvmIr.XStatementOrExpression>,
    private val debugInfo: List<String>
) : IrLibraryFile() {
    override fun declaration(index: Int): ProtoDeclaration {
        error("This method is never supposed to be called")
    }

    override fun type(index: Int): ProtoType = types[index]
    override fun signature(index: Int): ProtoIdSignature = signatures[index]
    override fun string(index: Int): String = strings[index]
    override fun debugInfo(index: Int): String = debugInfo[index]

    override fun expressionBody(index: Int): ProtoExpression =
        bodies[index].also { require(it.hasExpression()) }.expression

    override fun statementBody(index: Int): ProtoStatement =
        bodies[index].also { require(it.hasStatement()) }.statement
}

private fun referencePublicSymbol(
    symbolTable: SymbolTable,
    descriptorFinder: DescriptorByIdSignatureFinderImpl,
    idSig: IdSignature,
    symbolKind: BinarySymbolData.SymbolKind
): IrSymbol {
    with(symbolTable) {
        val descriptor = descriptorFinder.findDescriptorBySignature(idSig)
        return if (descriptor != null && false) {
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
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClass(idSig)
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructor(idSig)
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntry(idSig)
                BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL, BinarySymbolData.SymbolKind.FIELD_SYMBOL
                    -> referenceField(idSig)
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunction(idSig)
                BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAlias(idSig)
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referenceProperty(idSig)
                BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameter(idSig)
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
    toplevel.acceptChildrenVoid(
        object : IrElementVisitorVoid {
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
