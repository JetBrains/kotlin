/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.resolve.providers.FirCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock

class PackagePartsCacheData(
    val proto: ProtoBuf.Package,
    val context: FirDeserializationContext,
    val extra: Extra? = null,
) {
    /**
     * Marker interface for 'extra' data that can be attached to a given [PackagePartsCacheData].
     * Example: This can be used by a [FirSymbolProvider] to attach data (like the library that this package part came from) to
     * this particular package
     *
     * @see PackagePartsCacheData.extra
     */
    interface Extra

    val topLevelFunctionNameIndex: Map<Name, List<Int>> by lazy {
        proto.functionList.withIndex()
            .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
    }
    val topLevelPropertyNameIndex: Map<Name, List<Int>> by lazy {
        proto.propertyList.withIndex()
            .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
    }
    val typeAliasNameIndex: Map<Name, List<Int>> by lazy {
        proto.typeAliasList.withIndex()
            .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
    }
}

abstract class LibraryPathFilter {
    abstract fun accepts(path: Path?): Boolean

    object TakeAll : LibraryPathFilter() {
        override fun accepts(path: Path?): Boolean {
            return true
        }
    }

    class LibraryList(libs: Set<Path>) : LibraryPathFilter() {
        val libs: Set<Path> = libs.mapTo(mutableSetOf()) { it.normalize() }

        override fun accepts(path: Path?): Boolean {
            if (path == null) return false
            val isPathAbsolute = path.isAbsolute
            val realPath by lazy(LazyThreadSafetyMode.NONE) { path.toRealPath() }
            return libs.any {
                when {
                    it.isAbsolute && !isPathAbsolute -> realPath.startsWith(it)
                    !it.isAbsolute && isPathAbsolute -> path.startsWith(it.toRealPath())
                    else -> path.startsWith(it)
                }
            }
        }
    }
}

typealias DeserializedClassPostProcessor = (FirRegularClassSymbol) -> Unit

typealias DeserializedTypeAliasPostProcessor = (FirTypeAliasSymbol) -> Unit

abstract class AbstractFirDeserializedSymbolProvider(
    session: FirSession,
    val moduleDataProvider: ModuleDataProvider,
    val kotlinScopeProvider: FirKotlinScopeProvider,
    val defaultDeserializationOrigin: FirDeclarationOrigin,
    private val serializerExtensionProtocol: SerializerExtensionProtocol
) : FirSymbolProvider(session) {
    // ------------------------ Caches ------------------------

    /**
     * [packageNamesForNonClassDeclarations] might contain names of packages containing type aliases, on top of packages containing
     * callables, so it's not the same as `symbolNamesProvider.getPackageNamesWithTopLevelCallables` and cannot be replaced by it.
     */
    private val packageNamesForNonClassDeclarations: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computePackageSetWithNonClassDeclarations()
    }

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirCachedSymbolNamesProvider(session) {
        override fun computePackageNames(): Set<String>? = null

        override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false

        override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? = null

        override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? {
            val classesInPackage = knownTopLevelClassesInPackage(packageFqName)?.mapToSetOrEmpty { Name.identifier(it) } ?: return null

            if (packageFqName.asString() !in packageNamesForNonClassDeclarations) return classesInPackage

            val typeAliasNames = typeAliasesNamesByPackage.getValue(packageFqName)
            if (typeAliasNames.isEmpty()) return classesInPackage

            return buildSet {
                addAll(classesInPackage)
                addAll(typeAliasNames)
            }
        }

        override val hasSpecificCallablePackageNamesComputation: Boolean get() = true

        override fun getPackageNamesWithTopLevelCallables(): Set<String> = packageNamesForNonClassDeclarations

        override fun computePackageNamesWithTopLevelCallables(): Set<String> = packageNamesForNonClassDeclarations

        override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name> =
            getPackageParts(packageFqName).flatMapTo(mutableSetOf()) {
                it.topLevelFunctionNameIndex.keys + it.topLevelPropertyNameIndex.keys
            }
    }

    private val typeAliasesNamesByPackage: FirCache<FqName, Set<Name>, Nothing?> =
        session.firCachesFactory.createCache { fqName: FqName ->
            getPackageParts(fqName).flatMapTo(mutableSetOf()) { it.typeAliasNameIndex.keys }
        }

    private val packagePartsCache = session.firCachesFactory.createCache(::tryComputePackagePartInfos)

    private val typeAliasCache: FirCache<ClassId, FirTypeAliasSymbol?, FirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId, _ -> findAndDeserializeTypeAlias(classId) },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            }
        )

    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, FirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId, context -> findAndDeserializeClass(classId, context) },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            },
            sharedClassComputationLock,
        )

    private val functionCache = session.firCachesFactory.createCache(::loadFunctionsByCallableId)
    private val propertyCache = session.firCachesFactory.createCache(::loadPropertiesByCallableId)

    // ------------------------ Abstract members ------------------------

    /**
     * A shared computation lock for the value computation of the provider's class cache.
     *
     * @see org.jetbrains.kotlin.fir.caches.FirCachesFactory.createCacheWithPostCompute
     */
    protected open val sharedClassComputationLock: ReentrantLock?
        get() = null

    protected abstract fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData>

    // Return full package names that might be not empty (have some non-class declarations) in this provider
    // In JVM, it's expensive to compute all the packages that might contain a Java class among dependencies
    // But, as we have all the metadata, we may be sure about top-level callables and type aliases
    // This method should only be used for sake of optimization to avoid having too many empty-list/null values in our caches
    protected abstract fun computePackageSetWithNonClassDeclarations(): Set<String>

    protected abstract fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String>?

    protected abstract fun extractClassMetadata(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): ClassMetadataFindResult?

    protected abstract fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean

    // ------------------------ Deserialization methods ------------------------

    sealed class ClassMetadataFindResult {
        data class NoMetadata(
            val classPostProcessor: DeserializedClassPostProcessor
        ) : ClassMetadataFindResult()

        data class Metadata(
            val nameResolver: NameResolver,
            val classProto: ProtoBuf.Class,
            val annotationDeserializer: AbstractAnnotationDeserializer?,
            val moduleData: FirModuleData?,
            val sourceElement: DeserializedContainerSource?,
            val classPostProcessor: DeserializedClassPostProcessor?,
            val flexibleTypeFactory: FirTypeDeserializer.FlexibleTypeFactory,
        ) : ClassMetadataFindResult()
    }

    private fun tryComputePackagePartInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return computePackagePartsInfos(packageFqName)
    }

    private fun findAndDeserializeTypeAlias(classId: ClassId): Pair<FirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        return getPackageParts(classId.packageFqName).firstNotNullOfOrNull { part ->
            val ids = part.typeAliasNameIndex[classId.shortClassName]
            if (ids == null || ids.isEmpty()) return@firstNotNullOfOrNull null
            val aliasProto = part.proto.getTypeAlias(ids.single())
            val postProcessor: DeserializedTypeAliasPostProcessor = { part.context.memberDeserializer.loadTypeAlias(aliasProto, it) }
            FirTypeAliasSymbol(classId) to postProcessor
        } ?: (null to null)
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): Pair<FirRegularClassSymbol?, DeserializedClassPostProcessor?> {
        return when (val result = extractClassMetadata(classId, parentContext)) {
            is ClassMetadataFindResult.NoMetadata -> FirRegularClassSymbol(classId) to result.classPostProcessor
            is ClassMetadataFindResult.Metadata -> {
                val (nameResolver, classProto, annotationDeserializer, moduleData, sourceElement, postProcessor) = result
                moduleData ?: return null to null
                val symbol = FirRegularClassSymbol(classId)
                deserializeClassToSymbol(
                    classId,
                    classProto,
                    symbol,
                    nameResolver,
                    session,
                    moduleData,
                    annotationDeserializer,
                    result.flexibleTypeFactory,
                    kotlinScopeProvider,
                    serializerExtensionProtocol,
                    parentContext,
                    sourceElement,
                    origin = defaultDeserializationOrigin,
                    deserializeNestedClass = this::getClass,
                )
                symbol.fir.isNewPlaceForBodyGeneration = isNewPlaceForBodyGeneration(classProto)
                symbol to postProcessor
            }
            null -> null to null
        }
    }

    private fun loadFunctionsByCallableId(callableId: CallableId): List<FirNamedFunctionSymbol> {
        return getPackageParts(callableId.packageName).flatMap { part ->
            val functionIds = part.topLevelFunctionNameIndex[callableId.callableName] ?: return@flatMap emptyList()
            functionIds.map {
                val proto = part.proto.getFunction(it)
                val fir = part.context.memberDeserializer.loadFunction(
                    part.proto.getFunction(it),
                    deserializationOrigin = defaultDeserializationOrigin
                )
                loadFunctionExtensions(part, proto, fir)
                fir.symbol
            }
        }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId): List<FirPropertySymbol> {
        return getPackageParts(callableId.packageName).flatMap { part ->
            val propertyIds = part.topLevelPropertyNameIndex[callableId.callableName] ?: return@flatMap emptyList()
            propertyIds.map {
                val proto = part.proto.getProperty(it)
                val fir = part.context.memberDeserializer.loadProperty(proto)
                loadPropertyExtensions(part, proto, fir)
                fir.symbol
            }
        }
    }

    open fun loadFunctionExtensions(
        packagePart: PackagePartsCacheData, proto: ProtoBuf.Function, fir: FirFunction,
    ) {
    }

    open fun loadPropertyExtensions(
        packagePart: PackagePartsCacheData, proto: ProtoBuf.Property, fir: FirProperty,
    ) {
    }

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> =
        packagePartsCache.getValue(packageFqName)

    protected fun getClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): FirRegularClassSymbol? {
        val parentClassId = classId.outerClassId

        // Actually, the second "if" should be enough but the first one might work faster
        if (parentClassId == null && !symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null
        if (parentClassId != null && !symbolNamesProvider.mayHaveTopLevelClassifier(classId.outermostClassId)) return null

        if (parentContext == null && parentClassId != null) {
            val alreadyLoaded = classCache.getValueIfComputed(classId)
            if (alreadyLoaded != null) return alreadyLoaded
            // Load parent first in case correct `parentContext` is needed to deserialize the metadata of this class.
            getClass(parentClassId, null)
            // If that's the case, `classCache` should contain a value for `classId`.
        }
        return classCache.getValue(classId, parentContext)
    }

    private fun getTypeAlias(classId: ClassId): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null

        // Don't actually query FirCache when we're sure there are no relevant value
        // It helps to decrease the size of a cache thus leading to better query time
        val packageFqName = classId.packageFqName
        if (packageFqName.asString() !in packageNamesForNonClassDeclarations) return null
        if (classId.shortClassName !in typeAliasesNamesByPackage.getValue(packageFqName)) return null

        return typeAliasCache.getValue(classId)
    }

    // ------------------------ SymbolProvider methods ------------------------

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getCallables(callableId)
        destination += propertyCache.getCallables(callableId)
    }

    private fun <C : FirCallableSymbol<*>> FirCache<CallableId, List<C>, Nothing?>.getCallables(id: CallableId): List<C> {
        // Don't actually query FirCache when we're sure there are no relevant value
        // It helps to decrease the size of a cache thus leading to better query time
        if (!symbolNamesProvider.mayHaveTopLevelCallable(id.packageName, id.callableName)) return emptyList()
        return getValue(id)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.getCallables(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.getCallables(CallableId(packageFqName, name))
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        /**
         * FIR has a contract that providers should return the first classifier with required classId they found.
         *
         * But on the other hand, there is a contract (from pre 1.0 times), that classes have more priority than typealiases,
         *   when search is performed in binary dependencies.
         *
         * And the second contract breaks the first in case, when there are two parts of a MPP library:
         * - one is platform one and contains `actual typealias`
         * - second is common one and contains `expect class`
         *
         * In this case, by the first contract, provider will return `expect class`, but FIR expects that `actual typealias` will be found
         * So, to avoid this problem, we need to search for typealias in case, when `expect class` was found
         *
         * For details see KT-65840
         */
        val clazz = getClass(classId)
        if (clazz != null && !clazz.isExpect) {
            return clazz
        }
        return getTypeAlias(classId) ?: clazz
    }
}
