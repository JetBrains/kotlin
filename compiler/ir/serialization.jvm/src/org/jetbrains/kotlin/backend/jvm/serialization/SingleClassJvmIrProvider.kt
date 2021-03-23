/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.overrides.DefaultFakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.jvm.serialization.proto.JvmIr
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerIr
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.ByteString
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration

/* Reads serialized IR from annotations in classfiles */
class SingleClassJvmIrProvider(
    val moduleDescriptor: ModuleDescriptor,
    val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val irFactory: IrFactory,
    val fileFinder: VirtualFileFinder,
) : IrProvider, FileLocalAwareLinker {

    val descriptorFinder =
        DescriptorByIdSignatureFinder(moduleDescriptor, JvmManglerDesc(), DescriptorByIdSignatureFinder.LookupMode.MODULE_WITH_DEPENDENCIES)

    val packageFragments = mutableMapOf<FqName, IrExternalPackageFragment>()

    val facadeClassMap = mutableMapOf<IdSignature, Name>()

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        val facadeName = findFacadeClassName(symbol)
        if (facadeName != null)
            loadIrFileFromFacadeClass(facadeName)
        else
            symbol.signature?.let { loadToplevelClassBySignature(it) }
        return if (symbol.isBound) (symbol.owner as IrDeclaration) else null
    }

    private fun loadIrFileFromFacadeClass(facadeName: FqName) {
        val vfile = fileFinder.findVirtualFileWithHeader(ClassId.topLevel(facadeName)) ?: return
        val binaryClass = KotlinBinaryClassCache.getKotlinBinaryClassOrClassFileContent(vfile)?.toKotlinJvmBinaryClass() ?: return
        val classHeader = binaryClass.classHeader
        if (classHeader.serializedIr == null || classHeader.serializedIr!!.isEmpty()) return

        val containerSource = getJvmPackagePartSource(binaryClass)

        val irProto = JvmIr.JvmIrFile.parseFrom(classHeader.serializedIr)
        val packageFragment = getPackageFragment(facadeName.parent(), containerSource)
        val declarationDeserializer = getDeclarationDeserializer(packageFragment, irProto.auxTables)
        val facadeClass = irFactory.buildClass {
            name = facadeName.shortName()
            origin = IrDeclarationOrigin.SYNTHETIC_FILE_CLASS
        }.apply {
            parent = packageFragment
            createParameterDeclarations()
            // TODO: annotations
        }
        for (declProto in irProto.declarationList) {
            val declaration = declarationDeserializer.deserializeDeclaration(declProto)
            facadeClass.addMember(declaration)
            declaration.parent = facadeClass
        }
    }

    private fun loadToplevelClassBySignature(signature: IdSignature) {
        if (signature !is IdSignature.PublicSignature) return
        val classId = ClassId.topLevel(signature.packageFqName().child(Name.identifier(signature.firstNameSegment)))
        val toplevelDescriptor = moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: return

        /* TODO: should not be needed after we deal with fake overrides. */
        if (symbolTable.referenceClass(toplevelDescriptor).isBound) return

        val source = toplevelDescriptor.source as? KotlinJvmBinarySourceElement ?: return
        val classHeader = source.binaryClass.classHeader
        if (classHeader.serializedIr == null || classHeader.serializedIr!!.isEmpty()) return

        val irProto = JvmIr.JvmIrClass.parseFrom(classHeader.serializedIr)
        val packageFragment = getPackageFragment(signature.packageFqName(), source)
        val declarationDeserializer = getDeclarationDeserializer(packageFragment, irProto.auxTables)

        /* Need to create a ProtoDeclaration. */
        val protoDecl = ProtoDeclaration.newBuilder().run {
            this.irClass = irProto.irClass
            build()
        }

        declarationDeserializer.deserializeDeclaration(protoDecl)
    }

    private fun getDeclarationDeserializer(packageFragment: IrPackageFragment, auxTables: JvmIr.AuxTables): IrDeclarationDeserializer {
        val libraryFile = IrLibraryFileFromAnnotation(
            auxTables.typeList,
            auxTables.signatureList,
            auxTables.stringList,
            auxTables.bodyList
        )

        val symbolDeserializer = IrSymbolDeserializer(
            symbolTable,
            libraryFile,
            /* TODO */ actuals = emptyList(),
            enqueueLocalTopLevelDeclaration = {}, // just link to it in symbolTable
            handleExpectActualMapping = { _, _ -> TODO() },
            deserializePublicSymbol = ::referencePublicSymbol
        )

        populateFacadeClassMap(auxTables, libraryFile, symbolDeserializer)

        return IrDeclarationDeserializer(
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

    private fun getPackageFragment(fqName: FqName, containerSource: DeserializedContainerSource?): IrExternalPackageFragment =
        IrExternalPackageFragmentImpl(
            IrExternalPackageFragmentSymbolImpl(object : PackageFragmentDescriptorImpl(moduleDescriptor, fqName) {
                override fun getMemberScope() = MemberScope.Empty
            }),
            fqName,
            containerSource
        )

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

    private fun populateFacadeClassMap(auxTables: JvmIr.AuxTables, libraryFile: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer) {
        for (protoFacadeClassInfo in auxTables.facadeClassInfoList) {
            val signature = symbolDeserializer.deserializeIdSignature(protoFacadeClassInfo.signature)
            val name = Name.identifier(libraryFile.deserializeString(protoFacadeClassInfo.facadeClassName))
            facadeClassMap[signature] = name
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun findFacadeClassName(symbol: IrSymbol): FqName? {
        if (symbol.hasDescriptor) {
            var descriptor = symbol.descriptor
            while (descriptor.containingDeclaration !is PackageFragmentDescriptor) {
                descriptor = descriptor.containingDeclaration!!
            }
            if (descriptor is ClassDescriptor) return null
            val source = (descriptor as? DescriptorWithContainerSource)?.containerSource as? JvmPackagePartSource ?: return null
            val facadeName = source.facadeClassName ?: source.className
            return facadeName.fqNameForTopLevelClassMaybeWithDollars
        } else {
            val signature = symbol.signature ?: return null
            val facadeName = facadeClassMap[signature] ?: return null
            return signature.packageFqName().child(facadeName)
        }
    }

    // Copied from KotlinDeserializedJvmSymbolsProvider.
    private fun getJvmPackagePartSource(binaryClass: KotlinJvmBinaryClass): JvmPackagePartSource {
        val header = binaryClass.classHeader
        val data = header.data ?: header.incompatibleData ?: error("Should not happen")
        val strings = header.strings ?: error("Should not happen")
        val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(data, strings)
        return JvmPackagePartSource(
            binaryClass, packageProto, nameResolver,
            binaryClass.incompatibility, binaryClass.isPreReleaseInvisible
        )
    }

    private val KotlinJvmBinaryClass.incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>?
        get() {
            // TODO: skipMetadataVersionCheck
            if (classHeader.metadataVersion.isCompatible()) return null
            return IncompatibleVersionErrorData(classHeader.metadataVersion, JvmMetadataVersion.INSTANCE, location, classId)
        }

    private val KotlinJvmBinaryClass.isPreReleaseInvisible: Boolean
        get() = classHeader.isPreRelease


    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? = null
    override fun tryReferencingSimpleFunctionByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrSimpleFunctionSymbol? =
        null
}