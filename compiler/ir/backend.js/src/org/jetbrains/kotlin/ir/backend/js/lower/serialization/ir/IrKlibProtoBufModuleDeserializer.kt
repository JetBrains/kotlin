/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataSerializerProtocol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import java.io.File

class IrKlibProtoBufModuleDeserializer(
    currentModule: ModuleDescriptor,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    private val forwardModuleDescriptor: ModuleDescriptor?)
        : IrModuleDeserializer(logger, builtIns, symbolTable) {

    private val deserializedSymbols = mutableMapOf<UniqIdKey, IrSymbol>()
    val knownBuiltInsDescriptors = mutableMapOf<DeclarationDescriptor, UniqId>()
    private val reachableTopLevels = mutableSetOf<UniqIdKey>()
    private val deserializedTopLevels = mutableSetOf<UniqIdKey>()
    private val forwardDeclarations = mutableSetOf<IrSymbol>()

    private var deserializedModuleDescriptor: ModuleDescriptor? = null
    private var deserializedModuleProtoSymbolTables = mutableMapOf<ModuleDescriptor, IrKlibProtoBuf.IrSymbolTable>()
    private var deserializedModuleProtoStringTables = mutableMapOf<ModuleDescriptor, IrKlibProtoBuf.StringTable>()
    private var deserializedModuleProtoTypeTables = mutableMapOf<ModuleDescriptor, IrKlibProtoBuf.IrTypeTable>()

    val resolvedForwardDeclarations = mutableMapOf<UniqIdKey, UniqIdKey>()

    private val descriptorReferenceDeserializer = object : DescriptorReferenceDeserializer(currentModule, resolvedForwardDeclarations) {
        override fun resolveSpecialDescriptor(fqn: FqName) = builtIns.builtIns.getBuiltInClassByFqName(fqn)

        override fun checkIfSpecialDescriptorId(id: Long) =
            (FUNCTION_INDEX_START + BUILT_IN_UNIQ_ID_CLASS_OFFSET) <= id && id < (FUNCTION_INDEX_START + BUILT_IN_UNIQ_ID_GAP)

        override fun getDescriptorIdOrNull(descriptor: DeclarationDescriptor) =
            knownBuiltInsDescriptors[descriptor]?.index ?: if (isBuiltInFunction(descriptor))
                FUNCTION_INDEX_START + builtInFunctionId(descriptor)
            else null

    }

    private val descriptorToDirectoryMap = mutableMapOf<ModuleDescriptor, File>()

    private fun irDirectory(m: ModuleDescriptor): File = descriptorToDirectoryMap[m]!!

    private val FUNCTION_INDEX_START: Long

    init {
        // TODO: think about order
        var currentIndex = 0x1_0000_0000L
        builtIns.knownBuiltins.forEach {
            require(it is IrFunction)
            deserializedSymbols.put(UniqIdKey(null, UniqId(currentIndex, isLocal = false)), it.symbol)
            assert(symbolTable.referenceSimpleFunction(it.descriptor) == it.symbol)
            currentIndex++
        }

        FUNCTION_INDEX_START = currentIndex
    }

    private fun referenceDeserializedSymbol(proto: IrKlibProtoBuf.IrSymbolData, descriptor: DeclarationDescriptor?): IrSymbol = when (proto.kind) {
        IrKlibProtoBuf.IrSymbolKind.ANONYMOUS_INIT_SYMBOL ->
            IrAnonymousInitializerSymbolImpl(
                descriptor as ClassDescriptor?
                    ?: WrappedClassDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.CLASS_SYMBOL ->
            symbolTable.referenceClass(
                descriptor as ClassDescriptor?
                    ?: WrappedClassDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.CONSTRUCTOR_SYMBOL ->
            symbolTable.referenceConstructor(
                descriptor as ClassConstructorDescriptor?
                    ?: WrappedClassConstructorDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.TYPE_PARAMETER_SYMBOL ->
            symbolTable.referenceTypeParameter(
                descriptor as TypeParameterDescriptor?
                    ?: WrappedTypeParameterDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.ENUM_ENTRY_SYMBOL ->
            symbolTable.referenceEnumEntry(
                descriptor as ClassDescriptor?
                    ?: WrappedEnumEntryDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.STANDALONE_FIELD_SYMBOL ->
            symbolTable.referenceField(WrappedFieldDescriptor())

        IrKlibProtoBuf.IrSymbolKind.FIELD_SYMBOL ->
            symbolTable.referenceField(
                descriptor as PropertyDescriptor?
                    ?: WrappedPropertyDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.FUNCTION_SYMBOL ->
            symbolTable.referenceSimpleFunction(
                descriptor as FunctionDescriptor?
                    ?: WrappedSimpleFunctionDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.VARIABLE_SYMBOL ->
            IrVariableSymbolImpl(
                descriptor as VariableDescriptor?
                    ?: WrappedVariableDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.VALUE_PARAMETER_SYMBOL ->
            IrValueParameterSymbolImpl(
                descriptor as ParameterDescriptor?
                    ?: WrappedValueParameterDescriptor()
            )
        IrKlibProtoBuf.IrSymbolKind.RECEIVER_PARAMETER_SYMBOL ->
            IrValueParameterSymbolImpl(
                descriptor as ParameterDescriptor? ?: WrappedReceiverParameterDescriptor()
            )
        else -> TODO("Unexpected classifier symbol kind: ${proto.kind}")
    }

    override fun deserializeIrSymbol(proto: IrKlibProtoBuf.IrSymbol): IrSymbol {
        val symbolData =
            deserializedModuleProtoSymbolTables[deserializedModuleDescriptor]!!.getSymbols(proto.index)
        return deserializeIrSymbolData(symbolData)
    }

    override fun deserializeIrType(proto: IrKlibProtoBuf.IrTypeIndex): IrType {
        val typeData =
            deserializedModuleProtoTypeTables[deserializedModuleDescriptor]!!.getTypes(proto.index)
        return deserializeIrTypeData(typeData)
    }

    override fun deserializeString(proto: IrKlibProtoBuf.String) =
        deserializedModuleProtoStringTables[deserializedModuleDescriptor]!!.getStrings(proto.index)

    fun deserializeIrSymbolData(proto: IrKlibProtoBuf.IrSymbolData): IrSymbol {
        val key = proto.uniqId.uniqIdKey(deserializedModuleDescriptor!!)
        val topLevelKey = proto.topLevelUniqId.uniqIdKey(deserializedModuleDescriptor!!)

        if (!deserializedTopLevels.contains(topLevelKey)) reachableTopLevels.add(topLevelKey)

        val symbol = deserializedSymbols.getOrPut(key) {
            val descriptor = if (proto.hasDescriptorReference()) {
                deserializeDescriptorReference(proto.descriptorReference)
            } else {
                null
            }

            resolvedForwardDeclarations[key]?.let {
                if (!deserializedTopLevels.contains(it)) reachableTopLevels.add(it) // Assuming forward declarations are always top levels.
            }

            referenceDeserializedSymbol(proto, descriptor)
        }

        if (symbol.descriptor is ClassDescriptor &&
            symbol.descriptor !is WrappedDeclarationDescriptor<*>/* &&
            symbol.descriptor.module.isForwardDeclarationModule*/
        ) {
            forwardDeclarations.add(symbol)
        }

        return symbol
    }

    override fun deserializeDescriptorReference(proto: IrKlibProtoBuf.DescriptorReference) =
        descriptorReferenceDeserializer.deserializeDescriptorReference(
            deserializeString(proto.packageFqName),
            deserializeString(proto.classFqName),
            deserializeString(proto.name),
            if (proto.hasUniqId()) proto.uniqId.index else null,
            isEnumEntry = proto.isEnumEntry,
            isEnumSpecial = proto.isEnumSpecial,
            isDefaultConstructor = proto.isDefaultConstructor,
            isFakeOverride = proto.isFakeOverride,
            isGetter = proto.isGetter,
            isSetter = proto.isSetter
        )

    private val ByteArray.codedInputStream: org.jetbrains.kotlin.protobuf.CodedInputStream
        get() {
            val codedInputStream = org.jetbrains.kotlin.protobuf.CodedInputStream.newInstance(this)
            codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
            return codedInputStream
        }

    private val reversedFileIndex = mutableMapOf<UniqIdKey, IrFile>()

    private val UniqIdKey.moduleOfOrigin get() =
        this.moduleDescriptor ?: reversedFileIndex[this]?.packageFragmentDescriptor?.containingDeclaration

    private fun deserializeTopLevelDeclaration(uniqIdKey: UniqIdKey): IrDeclaration {
        val proto = loadTopLevelDeclarationProto(uniqIdKey)
        return deserializeDeclaration(proto, reversedFileIndex[uniqIdKey]!!)
    }

    private fun loadTopLevelDeclarationProto(uniqIdKey: UniqIdKey): IrKlibProtoBuf.IrDeclaration {
        val file = File(irDirectory(deserializedModuleDescriptor!!), uniqIdKey.uniqId.declarationFileName)
        return IrKlibProtoBuf.IrDeclaration.parseFrom(file.readBytes().codedInputStream, JsKlibMetadataSerializerProtocol.extensionRegistry)
    }

    private fun findDeserializedDeclarationForDescriptor(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
        val topLevelDescriptor = descriptor.findTopLevelDescriptor()

//        if (topLevelDescriptor.module.isForwardDeclarationModule) return null

        if (topLevelDescriptor !is DeserializedClassDescriptor && topLevelDescriptor !is DeserializedCallableMemberDescriptor) {
            return null
        }

        val descriptorUniqId = topLevelDescriptor.getUniqId()
            ?: error("could not get descriptor uniq id for $topLevelDescriptor")
        val uniqId = UniqId(descriptorUniqId.index, isLocal = false)
        val topLevelKey = UniqIdKey(topLevelDescriptor.module, uniqId)

        // This top level descriptor doesn't have a serialized IR declaration.
        if (topLevelKey.moduleOfOrigin == null) return null

        reachableTopLevels.add(topLevelKey)

        do {
            val key = reachableTopLevels.first()

            if (deserializedSymbols[key]?.isBound == true ||
                // The key.moduleOrigin is null for uniqIds that we haven't seen in any of the library headers.
                // Just skip it for now and handle it elsewhere.
                key.moduleOfOrigin == null) {

                reachableTopLevels.remove(key)
                deserializedTopLevels.add(key)
                continue
            }

            deserializedModuleDescriptor = key.moduleOfOrigin
            val reachable = deserializeTopLevelDeclaration(key)
            reversedFileIndex[key]!!.declarations.add(reachable)

            reachableTopLevels.remove(key)
            deserializedTopLevels.add(key)
        } while (reachableTopLevels.isNotEmpty())

        return topLevelDescriptor
    }

    override fun findDeserializedDeclaration(symbol: IrSymbol): IrDeclaration? {

        if (!symbol.isBound) {
            val topLevelDesecriptor = findDeserializedDeclarationForDescriptor(symbol.descriptor)
            if (topLevelDesecriptor == null) return null
        }

        assert(symbol.isBound) {
            "findDeserializedDeclaration: symbol ${symbol} is unbound, descriptor = ${symbol.descriptor}, hash = ${symbol.descriptor.hashCode()}"
        }

        return symbol.owner as IrDeclaration
    }

    override fun findDeserializedDeclaration(propertyDescriptor: PropertyDescriptor): IrProperty? {
        val topLevelDesecriptor = findDeserializedDeclarationForDescriptor(propertyDescriptor)
        if (topLevelDesecriptor == null) return null

        return symbolTable.propertyTable[propertyDescriptor]
            ?: error("findDeserializedDeclaration: property descriptor $propertyDescriptor} is not present in propertyTable after deserialization}")
    }

    override fun declareForwardDeclarations() {
        if (forwardModuleDescriptor == null) return

        val packageFragments = forwardDeclarations.map { it.descriptor.findPackage() }.distinct()

        // We don't bother making a real IR module here, as we have no need in it any later.
        // All we need is just to declare forward declarations in the symbol table
        // In case you need a full fledged module, turn the forEach into a map and collect
        // produced files into an IrModuleFragment.

        packageFragments.forEach { packageFragment ->
            val symbol = IrFileSymbolImpl(packageFragment)
            val file = IrFileImpl(NaiveSourceBasedFileEntryImpl("forward declarations pseudo-file"), symbol)
            val symbols = forwardDeclarations
                .filter { !it.isBound }
                .filter { it.descriptor.findPackage() == packageFragment }
            val declarations = symbols.map {

                val classDescriptor = it.descriptor as ClassDescriptor
                val declaration = symbolTable.declareClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                        classDescriptor,
                        classDescriptor.modality
                ) { symbol: IrClassSymbol -> IrClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin, symbol) }
                .also {
                    it.parent = file
                }
                declaration

            }
            file.declarations.addAll(declarations)
        }
    }

    fun deserializeIrFile(fileProto: IrKlibProtoBuf.IrFile, moduleDescriptor: ModuleDescriptor, deserializeAllDeclarations: Boolean): IrFile {
        val fileEntry = NaiveSourceBasedFileEntryImpl(
            deserializeString(fileProto.fileEntry.name),
            fileProto.fileEntry.lineStartOffsetsList.toIntArray()
        )

        // TODO: we need to store "" in protobuf, I suppose. Or better yet, reuse fqname storage from metadata.
        val fqName = deserializeString(fileProto.fqName).let { if (it == "<root>") FqName.ROOT else FqName(it) }

        val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)

        val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
        val file = IrFileImpl(fileEntry, symbol, fqName)

        fileProto.declarationIdList.forEach {
            val uniqIdKey = it.uniqIdKey(moduleDescriptor)
            reversedFileIndex.put(uniqIdKey, file)

            if (deserializeAllDeclarations) {
                file.declarations.add(deserializeTopLevelDeclaration(uniqIdKey))
            }
        }

        return file
    }

    fun deserializeIrModule(proto: IrKlibProtoBuf.IrModule, moduleDescriptor: ModuleDescriptor, deserializeAllDeclarations: Boolean): IrModuleFragment {

        deserializedModuleDescriptor = moduleDescriptor
        deserializedModuleProtoSymbolTables.put(moduleDescriptor, proto.symbolTable)
        deserializedModuleProtoStringTables.put(moduleDescriptor, proto.stringTable)
        deserializedModuleProtoTypeTables.put(moduleDescriptor, proto.typeTable)

        val files = proto.fileList.map {
            deserializeIrFile(it, moduleDescriptor, deserializeAllDeclarations)
        }
        val module = IrModuleFragmentImpl(moduleDescriptor, builtIns, files)
        module.patchDeclarationParents(null)
        return module
    }

    fun deserializeIrModule(moduleDescriptor: ModuleDescriptor, byteArray: ByteArray, klibLocation: File, deserializeAllDeclarations: Boolean = false): IrModuleFragment {
        descriptorToDirectoryMap[moduleDescriptor] = File(klibLocation, "ir/")
        val proto = IrKlibProtoBuf.IrModule.parseFrom(byteArray.codedInputStream, JsKlibMetadataSerializerProtocol.extensionRegistry)
        return deserializeIrModule(proto, moduleDescriptor, deserializeAllDeclarations)
    }
}