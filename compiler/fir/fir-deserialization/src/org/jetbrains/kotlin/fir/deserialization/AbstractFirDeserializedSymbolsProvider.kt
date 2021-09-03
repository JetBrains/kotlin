/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName
import java.nio.file.Path

class PackagePartsCacheData(
    val proto: ProtoBuf.Package,
    val context: FirDeserializationContext,
) {
    val topLevelFunctionNameIndex by lazy {
        proto.functionList.withIndex()
            .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
    }
    val topLevelPropertyNameIndex by lazy {
        proto.propertyList.withIndex()
            .groupBy({ context.nameResolver.getName(it.value.name) }) { (index) -> index }
    }
    val typeAliasNameIndex by lazy {
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
            return libs.any { path.startsWith(it) }
        }
    }
}

typealias DeserializedClassPostProcessor = (FirRegularClassSymbol) -> Unit

abstract class AbstractFirDeserializedSymbolsProvider(
    session: FirSession,
    val moduleDataProvider: ModuleDataProvider,
    val kotlinScopeProvider: FirKotlinScopeProvider,
) : FirSymbolProvider(session) {
    // ------------------------ Caches ------------------------

    private val packagePartsCache = session.firCachesFactory.createCache(::tryComputePackagePartInfos)
    private val typeAliasCache = session.firCachesFactory.createCache(::findAndDeserializeTypeAlias)
    protected val classCache: FirCache<ClassId, FirRegularClassSymbol?, FirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId, context -> findAndDeserializeClass(classId, context) },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            }
        )

    private val functionCache = session.firCachesFactory.createCache(::loadFunctionsByCallableId)
    private val propertyCache = session.firCachesFactory.createCache(::loadPropertiesByCallableId)

    // ------------------------ Abstract members ------------------------

    protected abstract fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData>
    protected abstract fun extractClassMetadata(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): ClassMetadataFindResult?

    // ------------------------ Deserialization methods ------------------------

    sealed class ClassMetadataFindResult {
        data class Metadata(
            val nameResolver: NameResolver,
            val classProto: ProtoBuf.Class,
            val annotationDeserializer: AbstractAnnotationDeserializer,
            val containingLibraryPath: Path?,
            val sourceElement: DeserializedContainerSource,
            val classPostProcessor: DeserializedClassPostProcessor
        ) : ClassMetadataFindResult()

        object ShouldDeserializeViaParent : ClassMetadataFindResult()
    }

    private fun tryComputePackagePartInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return try {
            computePackagePartsInfos(packageFqName)
        } catch (e: ProcessCanceledException) {
            emptyList()
        }
    }

    private fun findAndDeserializeTypeAlias(classId: ClassId): FirTypeAliasSymbol? {
        return getPackageParts(classId.packageFqName).firstNotNullOfOrNull { part ->
            val ids = part.typeAliasNameIndex[classId.shortClassName]
            if (ids == null || ids.isEmpty()) return@firstNotNullOfOrNull null
            val aliasProto = ids.map { part.proto.getTypeAlias(it) }.single()
            part.context.memberDeserializer.loadTypeAlias(aliasProto).symbol
        }
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): Pair<FirRegularClassSymbol?, DeserializedClassPostProcessor?> {
        return when (val result = extractClassMetadata(classId, parentContext)) {
            is ClassMetadataFindResult.Metadata -> {
                val (nameResolver, classProto, annotationDeserializer, containingLibrary, sourceElement, postProcessor) = result
                val moduleData = moduleDataProvider.getModuleData(containingLibrary) ?: return null to null
                val symbol = FirRegularClassSymbol(classId)
                deserializeClassToSymbol(
                    classId,
                    classProto,
                    symbol,
                    nameResolver,
                    session,
                    moduleData,
                    annotationDeserializer,
                    kotlinScopeProvider,
                    parentContext,
                    sourceElement,
                    deserializeNestedClass = this::getClass,
                )
                symbol to postProcessor
            }
            ClassMetadataFindResult.ShouldDeserializeViaParent -> findAndDeserializeClassViaParent(classId) to null
            null -> null to null
        }
    }

    private fun findAndDeserializeClassViaParent(classId: ClassId): FirRegularClassSymbol? {
        val outerClassId = classId.outerClassId ?: return null
        //This will cause cyclic cache request that is highly observable in IDE (but not in the compiler - but possible SOE bug also)
        //To avoid it in IDE there is special implementation that forces load parent class before any nested class request:
        //[org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionFactory.KotlinDeserializedJvmSymbolsProviderForIde]
        getClass(outerClassId) ?: return null
        return classCache.getValueIfComputed(classId)
    }

    private fun loadFunctionsByCallableId(callableId: CallableId): List<FirNamedFunctionSymbol> {
        return getPackageParts(callableId.packageName).flatMap { part ->
            val functionIds = part.topLevelFunctionNameIndex[callableId.callableName] ?: return@flatMap emptyList()
            functionIds.map {
                part.context.memberDeserializer.loadFunction(part.proto.getFunction(it)).symbol
            }
        }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId): List<FirPropertySymbol> {
        return getPackageParts(callableId.packageName).flatMap { part ->
            val propertyIds = part.topLevelPropertyNameIndex[callableId.callableName] ?: return@flatMap emptyList()
            propertyIds.map {
                part.context.memberDeserializer.loadProperty(part.proto.getProperty(it)).symbol
            }
        }
    }

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> {
        return packagePartsCache.getValue(packageFqName)
    }

    protected open fun getClass(
        classId: ClassId,
        parentContext: FirDeserializationContext? = null
    ): FirRegularClassSymbol? {
        return classCache.getValue(classId, parentContext)
    }

    private fun getTypeAlias(
        classId: ClassId,
    ): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null
        return typeAliasCache.getValue(classId)
    }

    // ------------------------ SymbolProvider methods ------------------------

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getValue(callableId)
        destination += propertyCache.getValue(callableId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.getValue(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.getValue(CallableId(packageFqName, name))
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return getClass(classId) ?: getTypeAlias(classId)
    }

    override fun getPackage(fqName: FqName): FqName? = null
}
