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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isOneSegmentFQN
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName

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

typealias DeserializedClassPostProcessor = (FirRegularClassSymbol) -> Unit

abstract class AbstractFirDeserializedSymbolsProvider(
    session: FirSession,
    val kotlinScopeProvider: FirKotlinScopeProvider
) : FirSymbolProvider(session) {

    // ------------------------ Caches ------------------------

    private val packagePartsCache = session.firCachesFactory.createCache(::tryComputePackagePartInfos)
    private val typeAliasCache = session.firCachesFactory.createCache(::findAndDeserializeTypeAlias)
    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, FirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId, context -> findAndDeserializeClass(classId, context) },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            }
        )

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
            val sourceElement: DeserializedContainerSource,
            val classPostProcessor: DeserializedClassPostProcessor
        ) : ClassMetadataFindResult()

        class ClassWithoutMetadata(val symbol: FirRegularClassSymbol?) : ClassMetadataFindResult()

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
                val (nameResolver, classProto, annotationDeserializer, sourceElement, postProcessor) = result
                val symbol = FirRegularClassSymbol(classId)
                deserializeClassToSymbol(
                    classId, classProto, symbol, nameResolver, session,
                    annotationDeserializer,
                    kotlinScopeProvider,
                    parentContext,
                    sourceElement,
                    deserializeNestedClass = this::getClass,
                )
                symbol to postProcessor
            }
            is ClassMetadataFindResult.ClassWithoutMetadata -> result.symbol to null
            ClassMetadataFindResult.ShouldDeserializeViaParent -> findAndDeserializeClassViaParent(classId) to null
            null -> null to null
        }
    }

    private fun findAndDeserializeClassViaParent(classId: ClassId): FirRegularClassSymbol? {
        val outerClassId = classId.outerClassId ?: return null
        getClass(outerClassId) ?: return null
        return classCache.getValueIfComputed(classId)
    }

    private fun loadFunctionsByName(part: PackagePartsCacheData, name: Name): List<FirNamedFunctionSymbol> {
        val functionIds = part.topLevelFunctionNameIndex[name] ?: return emptyList()
        return functionIds.map {
            part.context.memberDeserializer.loadFunction(part.proto.getFunction(it)).symbol
        }
    }

    private fun loadPropertiesByName(part: PackagePartsCacheData, name: Name): List<FirPropertySymbol> {
        val propertyIds = part.topLevelPropertyNameIndex[name] ?: return emptyList()
        return propertyIds.map {
            part.context.memberDeserializer.loadProperty(part.proto.getProperty(it)).symbol
        }
    }

    // ------------------------ SymbolProvider methods ------------------------

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        getPackageParts(packageFqName).flatMapTo(destination) { part ->
            loadFunctionsByName(part, name) + loadPropertiesByName(part, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getPackageParts(packageFqName).flatMapTo(destination) { part ->
            loadFunctionsByName(part, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        getPackageParts(packageFqName).flatMapTo(destination) { part ->
            loadPropertiesByName(part, name)
        }
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return getClass(classId) ?: getTypeAlias(classId)
    }

    private fun getClass(
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

    private fun getPackageParts(packageFqName: FqName): Collection<PackagePartsCacheData> {
        return packagePartsCache.getValue(packageFqName)
    }

    override fun getPackage(fqName: FqName): FqName? = null
}
