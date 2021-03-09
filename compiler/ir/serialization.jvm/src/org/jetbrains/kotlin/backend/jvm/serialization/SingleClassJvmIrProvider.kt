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
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerIr
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration

/* Reads serialized IR from annotations in classfiles */
class SingleClassJvmIrProvider(
    val moduleDescriptor: ModuleDescriptor,
    val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val irFactory: IrFactory,
) : IrProvider, FileLocalAwareLinker {

    val descriptorFinder =
        DescriptorByIdSignatureFinder(moduleDescriptor, JvmManglerDesc(), DescriptorByIdSignatureFinder.LookupMode.MODULE_WITH_DEPENDENCIES)

    val packageFragments = mutableMapOf<FqName, IrExternalPackageFragment>()

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        symbol.signature?.let(::findToplevelClassBySignature)
        return if (symbol.isBound) (symbol.owner as IrDeclaration) else null
    }

    private fun findToplevelClassBySignature(signature: IdSignature): IrClass? {
        if (signature !is IdSignature.PublicSignature) return null
        val classId = ClassId.topLevel(signature.packageFqName().child(Name.identifier(signature.firstNameSegment)))
        val toplevelDescriptor = moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return null
        val classHeader =
            (toplevelDescriptor.source as? KotlinJvmBinarySourceElement)?.binaryClass?.classHeader ?: return null
        if (classHeader.serializedIr == null || classHeader.serializedIr!!.isEmpty()) return null

        val irProto = JvmIr.JvmIrClass.parseFrom(classHeader.serializedIr)

        /* Need to create a ProtoDeclaration. */
        val protoDecl = ProtoDeclaration.newBuilder().run {
            this.irClass = irProto.irClass
            build()
        }

        val libraryFile = IrLibraryFileFromAnnotation(
            irProto.auxTables.typeList,
            irProto.auxTables.signatureList,
            irProto.auxTables.stringList,
            irProto.auxTables.bodyList
        )

        val symbolDeserializer = IrSymbolDeserializer(
            symbolTable,
            libraryFile,
            /* TODO */ actuals = emptyList(),
            enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
            handleExpectActualMapping = { _, _ -> TODO() },
            deserializePublicSymbol = ::referencePublicSymbol
        )

        val packageFragment = getPackageFragment(signature.packageFqName())

        val declarationDeserializer = IrDeclarationDeserializer(
            irBuiltIns,
            symbolTable,
            irFactory,
            libraryFile,
            packageFragment,
            allowErrorNodes = false,
            deserializeInlineFunctions = true,
            deserializeBodies = true,
            symbolDeserializer = symbolDeserializer,
            platformFakeOverrideClassFilter = DefaultFakeOverrideClassFilter,
            fakeOverrideBuilder = FakeOverrideBuilder(
                linker = this,
                symbolTable,
                IdSignatureSerializer(JvmManglerIr),
                irBuiltIns
            )
        )

        return declarationDeserializer.deserializeDeclaration(protoDecl) as IrClass
    }

    fun id(x: Any?): Any? = x

    class IrLibraryFileFromAnnotation(
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

    private fun getPackageFragment(fqName: FqName): IrExternalPackageFragment = packageFragments.getOrPut(fqName) {
        IrExternalPackageFragmentImpl(
            IrExternalPackageFragmentSymbolImpl(object : PackageFragmentDescriptorImpl(moduleDescriptor, fqName) {
                override fun getMemberScope() = MemberScope.Empty
            }),
            fqName
        )
    }

    private fun referencePublicSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
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

    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? = null
    override fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol? =
        null
}