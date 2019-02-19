/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.backend.konan.descriptors.findTopLevelDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.konan.KonanSerializerProtocol

class KonanIrModuleDeserializer(
    currentModule: ModuleDescriptor,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    val forwardModuleDescriptor: ModuleDescriptor?)
        : IrModuleDeserializer(logger, builtIns, symbolTable) {

    val deserializedSymbols = mutableMapOf<UniqIdKey, IrSymbol>()
    val reachableTopLevels = mutableSetOf<UniqIdKey>()
    val deserializedTopLevels = mutableSetOf<UniqIdKey>()
    val forwardDeclarations = mutableSetOf<IrSymbol>()

    var deserializedModuleDescriptor: ModuleDescriptor? = null
    var deserializedModuleProtoSymbolTables = mutableMapOf<ModuleDescriptor, KonanIr.IrSymbolTable>()
    var deserializedModuleProtoStringTables = mutableMapOf<ModuleDescriptor, KonanIr.StringTable>()
    var deserializedModuleProtoTypeTables = mutableMapOf<ModuleDescriptor, KonanIr.IrTypeTable>()

    val resolvedForwardDeclarations = mutableMapOf<UniqIdKey, UniqIdKey>()
    val descriptorReferenceDeserializer = DescriptorReferenceDeserializer(currentModule, resolvedForwardDeclarations)

    init {
        var currentIndex = 0L
        builtIns.knownBuiltins.forEach {
            require(it is IrFunction)
            deserializedSymbols.put(UniqIdKey(null, UniqId(currentIndex, isLocal = false)), it.symbol)
            assert(symbolTable.referenceSimpleFunction(it.descriptor) == it.symbol)
            currentIndex++
        }
    }

    private fun referenceDeserializedSymbol(proto: KonanIr.IrSymbolData, descriptor: DeclarationDescriptor?): IrSymbol = when (proto.kind) {
        KonanIr.IrSymbolKind.ANONYMOUS_INIT_SYMBOL ->
            IrAnonymousInitializerSymbolImpl(
                descriptor as ClassDescriptor?
                    ?: WrappedClassDescriptor()
            )
        KonanIr.IrSymbolKind.CLASS_SYMBOL ->
            symbolTable.referenceClass(
                descriptor as ClassDescriptor?
                    ?: WrappedClassDescriptor()
            )
        KonanIr.IrSymbolKind.CONSTRUCTOR_SYMBOL ->
            symbolTable.referenceConstructor(
                descriptor as ClassConstructorDescriptor?
                    ?: WrappedClassConstructorDescriptor()
            )
        KonanIr.IrSymbolKind.TYPE_PARAMETER_SYMBOL ->
            symbolTable.referenceTypeParameter(
                descriptor as TypeParameterDescriptor?
                    ?: WrappedTypeParameterDescriptor()
            )
        KonanIr.IrSymbolKind.ENUM_ENTRY_SYMBOL ->
            symbolTable.referenceEnumEntry(
                descriptor as ClassDescriptor?
                    ?: WrappedEnumEntryDescriptor()
            )
        KonanIr.IrSymbolKind.STANDALONE_FIELD_SYMBOL ->
            symbolTable.referenceField(WrappedFieldDescriptor())

        KonanIr.IrSymbolKind.FIELD_SYMBOL ->
            symbolTable.referenceField(
                descriptor as PropertyDescriptor?
                    ?: WrappedPropertyDescriptor()
            )
        KonanIr.IrSymbolKind.FUNCTION_SYMBOL ->
            symbolTable.referenceSimpleFunction(
                descriptor as FunctionDescriptor?
                    ?: WrappedSimpleFunctionDescriptor()
            )
        KonanIr.IrSymbolKind.VARIABLE_SYMBOL ->
            IrVariableSymbolImpl(
                descriptor as VariableDescriptor?
                    ?: WrappedVariableDescriptor()
            )
        KonanIr.IrSymbolKind.VALUE_PARAMETER_SYMBOL ->
            IrValueParameterSymbolImpl(
                descriptor as ParameterDescriptor?
                    ?: WrappedValueParameterDescriptor()
            )
        KonanIr.IrSymbolKind.RECEIVER_PARAMETER_SYMBOL ->
            IrValueParameterSymbolImpl(
                descriptor as ParameterDescriptor? ?: WrappedReceiverParameterDescriptor()
            )
        else -> TODO("Unexpected classifier symbol kind: ${proto.kind}")
    }

    override fun deserializeIrSymbol(proto: KonanIr.IrSymbol): IrSymbol {
        val symbolData =
            deserializedModuleProtoSymbolTables[deserializedModuleDescriptor]!!.getSymbols(proto.index)
        return deserializeIrSymbolData(symbolData)
    }

    override fun deserializeIrType(proto: KonanIr.IrTypeIndex): IrType {
        val typeData =
            deserializedModuleProtoTypeTables[deserializedModuleDescriptor]!!.getTypes(proto.index)
        return deserializeIrTypeData(typeData)
    }

    override fun deserializeString(proto: KonanIr.String) =
        deserializedModuleProtoStringTables[deserializedModuleDescriptor]!!.getStrings(proto.index)

    fun deserializeIrSymbolData(proto: KonanIr.IrSymbolData): IrSymbol {
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
            symbol.descriptor !is WrappedDeclarationDescriptor<*> &&
            symbol.descriptor.module.isForwardDeclarationModule
        ) {
            forwardDeclarations.add(symbol)
        }

        return symbol
    }

    override fun deserializeDescriptorReference(proto: KonanIr.DescriptorReference): DeclarationDescriptor =
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
    
    private fun reader(moduleDescriptor: ModuleDescriptor, uniqId: UniqId) = moduleDescriptor.konanLibrary!!.irDeclaration(uniqId.index, uniqId.isLocal)

    private fun loadTopLevelDeclarationProto(uniqIdKey: UniqIdKey): KonanIr.IrDeclaration {
        val stream = reader(uniqIdKey.moduleOfOrigin!!, uniqIdKey.uniqId).codedInputStream
        return KonanIr.IrDeclaration.parseFrom(stream, KonanSerializerProtocol.extensionRegistry)
    }

    private fun findDeserializedDeclarationForDescriptor(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
        val topLevelDescriptor = descriptor.findTopLevelDescriptor()

        if (topLevelDescriptor.module.isForwardDeclarationModule) return null

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

    fun deserializeIrFile(fileProto: KonanIr.IrFile, moduleDescriptor: ModuleDescriptor, deseralizationStrategy: DeserializationStrategy): IrFile {
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

            if (deseralizationStrategy == DeserializationStrategy.ALL) {
                file.declarations.add(deserializeTopLevelDeclaration(uniqIdKey))
            }
        }

        val annotations = deserializeAnnotations(fileProto.annotations)
        file.annotations.addAll(annotations)


        if (deseralizationStrategy == DeserializationStrategy.EXPLICITLY_EXPORTED)
            fileProto.explicitlyExportedToCompilerList.forEach { deserializeIrSymbol(it) }

        return file
    }

    fun deserializeIrModuleHeader(proto: KonanIr.IrModule, moduleDescriptor: ModuleDescriptor, deserializationStrategy: DeserializationStrategy): IrModuleFragment {

        deserializedModuleDescriptor = moduleDescriptor
        deserializedModuleProtoSymbolTables.put(moduleDescriptor, proto.symbolTable)
        deserializedModuleProtoStringTables.put(moduleDescriptor, proto.stringTable)
        deserializedModuleProtoTypeTables.put(moduleDescriptor, proto.typeTable)

        val files = proto.fileList.map {
            deserializeIrFile(it, moduleDescriptor, deserializationStrategy)

        }
        val module = IrModuleFragmentImpl(moduleDescriptor, builtIns, files)
        module.patchDeclarationParents(null)
        return module
    }

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor, byteArray: ByteArray, deserializationStrategy: DeserializationStrategy = DeserializationStrategy.ONLY_REFERENCED): IrModuleFragment {
        val proto = KonanIr.IrModule.parseFrom(byteArray.codedInputStream, KonanSerializerProtocol.extensionRegistry)
        return deserializeIrModuleHeader(proto, moduleDescriptor, deserializationStrategy)
    }
}

enum class DeserializationStrategy {
    ONLY_REFERENCED,
    ALL,
    EXPLICITLY_EXPORTED
}