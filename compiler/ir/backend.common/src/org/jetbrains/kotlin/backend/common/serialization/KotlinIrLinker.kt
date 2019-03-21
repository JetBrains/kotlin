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
    val exportedDependencies: List<ModuleDescriptor>,
    private val forwardModuleDescriptor: ModuleDescriptor?,
    private val firstKnownBuiltinsIndex: Long
) : DescriptorUniqIdAware, IrDeserializer {

    protected val deserializedSymbols = mutableMapOf<UniqIdKey, IrSymbol>()
    private val reachableTopLevels = mutableSetOf<UniqIdKey>()
    private val deserializedTopLevels = mutableSetOf<UniqIdKey>()

    //TODO: This is Native specific. Eliminate me.
    private val forwardDeclarations = mutableSetOf<IrSymbol>()
    val resolvedForwardDeclarations = mutableMapOf<UniqIdKey, UniqIdKey>()

    protected val deserializersForModules = mutableMapOf<ModuleDescriptor, IrDeserializerForModule>()
    val fileAnnotations = mutableMapOf<IrFile, KotlinIr.Annotations>()

    inner class IrDeserializerForModule(
        private val moduleDescriptor: ModuleDescriptor,
        private val moduleProto: KotlinIr.IrModule,
        private val deserializationStrategy: DeserializationStrategy
    ) : IrModuleDeserializer(logger, builtIns, symbolTable) {

        private var moduleLoops = mutableMapOf<Int, IrLoopBase>()

        // This is a heavy initializer
        val module = deserializeIrModuleHeader(moduleProto)

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

                descriptor?.module?.let {
                    if (!deserializersForModules.containsKey(it) && !it.isForwardDeclarationModule) {
                        deserializeIrModuleHeader(it)!!
                    }
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
        // TODO: this is JS specific. Eliminate me.
        override fun getPrimitiveTypeOrNull(symbol: IrClassifierSymbol, hasQuestionMark: Boolean) =
            this@KotlinIrLinker.getPrimitiveTypeOrNull(symbol, hasQuestionMark)

        fun deserializeIrFile(fileProto: KotlinIr.IrFile): IrFile {

            val fileEntry = NaiveSourceBasedFileEntryImpl(
                this.deserializeString(fileProto.fileEntry.name),
                fileProto.fileEntry.lineStartOffsetsList.toIntArray()
            )

            // TODO: we need to store "" in protobuf, I suppose. Or better yet, reuse fqname storage from metadata.
            val fqName = this.deserializeString(fileProto.fqName)
                .let { if (it == "<root>") FqName.ROOT else FqName(it) }

            val packageFragmentDescriptor = EmptyPackageFragmentDescriptor(moduleDescriptor, fqName)

            val symbol = IrFileSymbolImpl(packageFragmentDescriptor)
            val file = IrFileImpl(fileEntry, symbol, fqName)

            // We deserialize file annotations on first file use.
            fileAnnotations.put(file, fileProto.annotations)

            fileProto.declarationIdList.forEach {
                val uniqIdKey = it.uniqIdKey(moduleDescriptor)
                reversedFileIndex.put(uniqIdKey, file)
            }

            when (deserializationStrategy) {
                DeserializationStrategy.EXPLICITLY_EXPORTED -> {
                    fileProto.explicitlyExportedToCompilerList.forEach {
                        val symbolProto = moduleProto.symbolTable.getSymbols(it.index)
                        reachableTopLevels.add(symbolProto.topLevelUniqId.uniqIdKey(moduleDescriptor))
                    }
                }
                DeserializationStrategy.ALL -> {
                    fileProto.declarationIdList.forEach {
                        val uniqIdKey = it.uniqIdKey(moduleDescriptor)
                        reachableTopLevels.add(uniqIdKey)
                    }
                }
                else -> error("Unixpected deserialization strategy")
            }

            return file
        }

        fun deserializeIrModuleHeader(
            proto: KotlinIr.IrModule
        ): IrModuleFragment {
            val files = proto.fileList.map {
                deserializeIrFile(it)
            }
            val module = IrModuleFragmentImpl(moduleDescriptor, builtIns, files)
            module.patchDeclarationParents(null)
            return module
        }
    }

    // TODO: this is JS specific. Eliminate me.
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
        return deserializersForModules[uniqIdKey.moduleOfOrigin]!!
            .deserializeDeclaration(proto, reversedFileIndex[uniqIdKey]!!)
    }

    protected abstract fun reader(moduleDescriptor: ModuleDescriptor, uniqId: UniqId): ByteArray

    private fun loadTopLevelDeclarationProto(uniqIdKey: UniqIdKey): KotlinIr.IrDeclaration {
        val stream = reader(uniqIdKey.moduleOfOrigin!!, uniqIdKey.uniqId).codedInputStream
        return KotlinIr.IrDeclaration.parseFrom(stream, newInstance())
    }

    private fun deserializeFileAnnotationsIfFirstUse(module: ModuleDescriptor, file: IrFile) {
        val annotations = fileAnnotations[file] ?: return
        file.annotations.addAll(deserializersForModules[module]!!.deserializeAnnotations(annotations))
        fileAnnotations.remove(file)
    }

    private fun deserializeAllReachableTopLevels() {
        do {
            val key = reachableTopLevels.first()
            val moduleOfOrigin = key.moduleOfOrigin

            if (deserializedSymbols[key]?.isBound == true ||
                // The key.moduleOrigin is null for uniqIds that we haven't seen in any of the library headers.
                // Just skip it for now and handle it elsewhere.
                moduleOfOrigin == null
            ) {

                reachableTopLevels.remove(key)
                deserializedTopLevels.add(key)
                continue
            }

            val reachable = deserializeTopLevelDeclaration(key)
            val file = reversedFileIndex[key]!!
            file.declarations.add(reachable)
            reachable.patchDeclarationParents(file)
            deserializeFileAnnotationsIfFirstUse(moduleOfOrigin, file)

            reachableTopLevels.remove(key)
            deserializedTopLevels.add(key)
        } while (reachableTopLevels.isNotEmpty())
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

        deserializeAllReachableTopLevels()
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

    // TODO: This is Native specific. Eliminate me.
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

    fun deserializeIrModuleHeader(
        moduleDescriptor: ModuleDescriptor,
        byteArray: ByteArray,
        deserializationStrategy: DeserializationStrategy = DeserializationStrategy.ONLY_REFERENCED
    ): IrModuleFragment {
        val deserializerForModule = deserializersForModules.getOrPut(moduleDescriptor) {
            val proto = KotlinIr.IrModule.parseFrom(byteArray.codedInputStream, newInstance())
            IrDeserializerForModule(moduleDescriptor, proto, deserializationStrategy)
        }
        // The IrModule and its IrFiles have been created during module initialization.
        return deserializerForModule.module
    }

    abstract val ModuleDescriptor.irHeader: ByteArray?

    fun deserializeIrModuleHeader(moduleDescriptor: ModuleDescriptor): IrModuleFragment? =
        // TODO: do we really allow libraries without any IR?
        moduleDescriptor.irHeader?.let { header ->
            // TODO: consider skip deserializing explicitly exported declarations for libraries.
            // Now it's not valid because of all dependencies that must be computed.
            val deserializationStrategy =
                if (exportedDependencies.contains(moduleDescriptor)) {
                    DeserializationStrategy.ALL
                } else {
                    DeserializationStrategy.EXPLICITLY_EXPORTED
                }
            deserializeIrModuleHeader(moduleDescriptor, header, deserializationStrategy)
        }
}

enum class DeserializationStrategy {
    ONLY_REFERENCED,
    ALL,
    EXPLICITLY_EXPORTED
}
