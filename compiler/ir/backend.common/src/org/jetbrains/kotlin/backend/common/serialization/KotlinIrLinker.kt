/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.impl.IrLoopBase
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite.newInstance
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

abstract class KotlinIrLinker(
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    private val forwardModuleDescriptor: ModuleDescriptor?,
    private val firstKnownBuiltinsIndex: Long
) : DescriptorUniqIdAware, IrDeserializer {

    protected val deserializedSymbols = mutableMapOf<UniqIdKey, IrSymbol>()
    private val reachableTopLevels = mutableSetOf<UniqIdKey>()
    private val deserializedTopLevels = mutableSetOf<UniqIdKey>()
    private val forwardDeclarations = mutableSetOf<IrSymbol>()

    private val deserializersForModules = mutableMapOf<ModuleDescriptor, IrDeserializerForModule>()

    val resolvedForwardDeclarations = mutableMapOf<UniqIdKey, UniqIdKey>()

    inner class IrDeserializerForModule(
        private val moduleDescriptor: ModuleDescriptor,
        private val moduleProto: KotlinIr.IrModule
    ) : IrModuleDeserializer(logger, builtIns, symbolTable) {

        private var moduleLoops = mutableMapOf<Int, IrLoopBase>()

        private fun referenceDeserializedSymbol(
            proto: KotlinIr.IrSymbolData,
            descriptor: DeclarationDescriptor?
        ): IrSymbol = when (proto.kind) {
            KotlinIr.IrSymbolKind.ANONYMOUS_INIT_SYMBOL ->
                IrAnonymousInitializerSymbolImpl(
                    descriptor as ClassDescriptor?
                        ?: WrappedClassDescriptor()
                )
            KotlinIr.IrSymbolKind.CLASS_SYMBOL ->
                symbolTable.referenceClass(
                    descriptor as ClassDescriptor?
                        ?: WrappedClassDescriptor()
                )
            KotlinIr.IrSymbolKind.CONSTRUCTOR_SYMBOL ->
                symbolTable.referenceConstructor(
                    descriptor as ClassConstructorDescriptor?
                        ?: WrappedClassConstructorDescriptor()
                )
            KotlinIr.IrSymbolKind.TYPE_PARAMETER_SYMBOL ->
                symbolTable.referenceTypeParameter(
                    descriptor as TypeParameterDescriptor?
                        ?: WrappedTypeParameterDescriptor()
                )
            KotlinIr.IrSymbolKind.ENUM_ENTRY_SYMBOL ->
                symbolTable.referenceEnumEntry(
                    descriptor as ClassDescriptor?
                        ?: WrappedEnumEntryDescriptor()
                )
            KotlinIr.IrSymbolKind.STANDALONE_FIELD_SYMBOL ->
                symbolTable.referenceField(WrappedFieldDescriptor())

            KotlinIr.IrSymbolKind.FIELD_SYMBOL ->
                symbolTable.referenceField(
                    descriptor as PropertyDescriptor?
                        ?: WrappedPropertyDescriptor()
                )
            KotlinIr.IrSymbolKind.FUNCTION_SYMBOL ->
                symbolTable.referenceSimpleFunction(
                    descriptor as FunctionDescriptor?
                        ?: WrappedSimpleFunctionDescriptor()
                )
            KotlinIr.IrSymbolKind.VARIABLE_SYMBOL ->
                IrVariableSymbolImpl(
                    descriptor as VariableDescriptor?
                        ?: WrappedVariableDescriptor()
                )
            KotlinIr.IrSymbolKind.VALUE_PARAMETER_SYMBOL ->
                IrValueParameterSymbolImpl(
                    descriptor as ParameterDescriptor?
                        ?: WrappedValueParameterDescriptor()
                )
            KotlinIr.IrSymbolKind.RECEIVER_PARAMETER_SYMBOL ->
                IrValueParameterSymbolImpl(
                    descriptor as ParameterDescriptor? ?: WrappedReceiverParameterDescriptor()
                )
            else -> TODO("Unexpected classifier symbol kind: ${proto.kind}")
        }

        override fun deserializeIrSymbol(proto: KotlinIr.IrSymbol): IrSymbol {
            val symbolData = moduleProto.symbolTable.getSymbols(proto.index)
            return deserializeIrSymbolData(symbolData)
        }

        override fun deserializeIrType(proto: KotlinIr.IrTypeIndex): IrType {
            val typeData = moduleProto.typeTable.getTypes(proto.index)
            return deserializeIrTypeData(typeData)
        }

        override fun deserializeString(proto: KotlinIr.String): String =
            moduleProto.stringTable.getStrings(proto.index)

        override fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoopBase) =
            moduleLoops.getOrPut(loopIndex, loopBuilder)

        private fun deserializeIrSymbolData(proto: KotlinIr.IrSymbolData): IrSymbol {
            val key = proto.uniqId.uniqIdKey(moduleDescriptor)
            val topLevelKey = proto.topLevelUniqId.uniqIdKey(moduleDescriptor)

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

        override fun deserializeDescriptorReference(proto: KotlinIr.DescriptorReference) =
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
                isSetter = proto.isSetter,
                isTypeParameter = proto.isTypeParameter
            )


        override fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean) =
            this@KotlinIrLinker.getPrimitiveTypeOrNull(symbol, hasQuestionMark)
    }

    protected open fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean): IrSimpleType? = null

    protected abstract val descriptorReferenceDeserializer: DescriptorReferenceDeserializer

    protected val indexAfterKnownBuiltins = loadKnownBuiltinSymbols()

    private fun loadKnownBuiltinSymbols(): Long {
        var currentIndex = firstKnownBuiltinsIndex
        builtIns.knownBuiltins.forEach {
            require(it is IrFunction)
            deserializedSymbols[UniqIdKey(null, UniqId(currentIndex, isLocal = false))] = it.symbol
            assert(symbolTable.referenceSimpleFunction(it.descriptor) == it.symbol)
            currentIndex++
        }
        return currentIndex
    }

    private val ByteArray.codedInputStream: org.jetbrains.kotlin.protobuf.CodedInputStream
        get() {
            val codedInputStream = org.jetbrains.kotlin.protobuf.CodedInputStream.newInstance(this)
            codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
            return codedInputStream
        }

    private val reversedFileIndex = mutableMapOf<UniqIdKey, IrFile>()

    private val UniqIdKey.moduleOfOrigin
        get() =
            this.moduleDescriptor ?: reversedFileIndex[this]?.packageFragmentDescriptor?.containingDeclaration

    private fun deserializeTopLevelDeclaration(uniqIdKey: UniqIdKey): IrDeclaration {
        val proto = loadTopLevelDeclarationProto(uniqIdKey)
        return deserializersForModules[uniqIdKey.moduleOfOrigin]!!.deserializeDeclaration(
            proto,
            reversedFileIndex[uniqIdKey]!!
        )
    }

    protected abstract fun reader(moduleDescriptor: ModuleDescriptor, uniqId: UniqId): ByteArray

    private fun loadTopLevelDeclarationProto(uniqIdKey: UniqIdKey): KotlinIr.IrDeclaration {
        val stream = reader(uniqIdKey.moduleOfOrigin!!, uniqIdKey.uniqId).codedInputStream
        return KotlinIr.IrDeclaration.parseFrom(stream, newInstance())
    }

    private fun findDeserializedDeclarationForDescriptor(descriptor: DeclarationDescriptor): DeclarationDescriptor? {
        val topLevelDescriptor = descriptor.findTopLevelDescriptor()

        // This is Native specific. Try to eliminate.
        if (topLevelDescriptor.module.isForwardDeclarationModule) return null

        if (topLevelDescriptor !is DeserializedClassDescriptor && topLevelDescriptor !is DeserializedCallableMemberDescriptor) {
            return null
        }

        val descriptorUniqId = topLevelDescriptor.getUniqId()
            ?: error("could not get descriptor uniq id for $topLevelDescriptor")
        val uniqId = UniqId(descriptorUniqId, isLocal = false)
        val topLevelKey = UniqIdKey(topLevelDescriptor.module, uniqId)

        // This top level descriptor doesn't have a serialized IR declaration.
        if (topLevelKey.moduleOfOrigin == null) return null

        reachableTopLevels.add(topLevelKey)

        do {
            val key = reachableTopLevels.first()

            if (deserializedSymbols[key]?.isBound == true ||
                // The key.moduleOrigin is null for uniqIds that we haven't seen in any of the library headers.
                // Just skip it for now and handle it elsewhere.
                key.moduleOfOrigin == null
            ) {

                reachableTopLevels.remove(key)
                deserializedTopLevels.add(key)
                continue
            }

            val reachable = deserializeTopLevelDeclaration(key)
            val file = reversedFileIndex[key]!!
            file.declarations.add(reachable)
            reachable.patchDeclarationParents(file)

            reachableTopLevels.remove(key)
            deserializedTopLevels.add(key)
        } while (reachableTopLevels.isNotEmpty())

        return topLevelDescriptor
    }

    override fun findDeserializedDeclaration(symbol: IrSymbol): IrDeclaration? {

        if (!symbol.isBound) {
            findDeserializedDeclarationForDescriptor(symbol.descriptor) ?: return null
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
                val declaration = symbolTable.declareClass(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
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

    fun deserializeIrFile(
        fileProto: KotlinIr.IrFile,
        moduleDescriptor: ModuleDescriptor,
        deseralizationStrategy: DeserializationStrategy
    ): IrFile {

        val moduleDeserializer = deserializersForModules[moduleDescriptor]!!

        val fileEntry = NaiveSourceBasedFileEntryImpl(
            moduleDeserializer.deserializeString(fileProto.fileEntry.name),
            fileProto.fileEntry.lineStartOffsetsList.toIntArray()
        )

        // TODO: we need to store "" in protobuf, I suppose. Or better yet, reuse fqname storage from metadata.
        val fqName = moduleDeserializer.deserializeString(fileProto.fqName)
            .let { if (it == "<root>") FqName.ROOT else FqName(it) }

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

        val annotations = moduleDeserializer.deserializeAnnotations(fileProto.annotations)
        file.annotations.addAll(annotations)


        if (deseralizationStrategy == DeserializationStrategy.EXPLICITLY_EXPORTED)
            fileProto.explicitlyExportedToCompilerList.forEach { moduleDeserializer.deserializeIrSymbol(it) }

        return file
    }

    fun deserializeIrModuleHeader(
        proto: KotlinIr.IrModule,
        moduleDescriptor: ModuleDescriptor,
        deserializationStrategy: DeserializationStrategy
    ): IrModuleFragment {

        deserializersForModules[moduleDescriptor] = IrDeserializerForModule(moduleDescriptor, proto)

        val files = proto.fileList.map {
            deserializeIrFile(it, moduleDescriptor, deserializationStrategy)
        }
        val module = IrModuleFragmentImpl(moduleDescriptor, builtIns, files)
        module.patchDeclarationParents(null)
        return module
    }

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        byteArray: ByteArray,
        deserializationStrategy: DeserializationStrategy = DeserializationStrategy.ONLY_REFERENCED
    ): IrModuleFragment {
        val proto = KotlinIr.IrModule.parseFrom(byteArray.codedInputStream, newInstance())
        return deserializeIrModuleHeader(proto, moduleDescriptor, deserializationStrategy)
    }
}

enum class DeserializationStrategy {
    ONLY_REFERENCED,
    ALL,
    EXPLICITLY_EXPORTED
}
