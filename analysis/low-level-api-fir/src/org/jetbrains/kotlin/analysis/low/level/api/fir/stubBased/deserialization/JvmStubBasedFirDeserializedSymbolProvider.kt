/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolProviderNameCache
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.deserialization.KotlinBuiltins
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment

typealias DeserializedTypeAliasPostProcessor = (FirTypeAliasSymbol) -> Unit

/**
 * [JvmStubBasedFirDeserializedSymbolProvider] works over existing stubs,
 * retrieving them by classId/callableId from [KotlinDeclarationProvider].
 *
 * It works in IDE only, in standalone mode works [JvmClassFileBasedSymbolProvider].
 *
 * Because it works over existing stubs, there is no need to keep huge protobuf in memory.
 * At the same time, there is no need to guess sources for fir elements anymore,
 * they are set during deserialization.
 *
 * Same as [JvmClassFileBasedSymbolProvider], resulting fir elements are already resolved.
 */
class JvmStubBasedFirDeserializedSymbolProvider(
    session: FirSession,
    moduleDataProvider: SingleModuleDataProvider,
    private val kotlinScopeProvider: FirKotlinScopeProvider,
    private val declarationProvider: KotlinDeclarationProvider
) : FirSymbolProvider(session) {
    private val moduleData = moduleDataProvider.getModuleData(null)
    private val packageSetWithTopLevelCallableDeclarations: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        declarationProvider.computePackageSetWithTopLevelCallableDeclarations()
    }

    private val namesByPackageCache = LLFirKotlinSymbolProviderNameCache(session, declarationProvider)

    private val typeAliasCache: FirCache<ClassId, FirTypeAliasSymbol?, StubBasedFirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId, _ -> findAndDeserializeTypeAlias(classId) },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            }
        )
    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, StubBasedFirDeserializationContext?> =
        session.firCachesFactory.createCache(
            createValue = { classId, context -> findAndDeserializeClass(classId, context) }
        )

    private val functionCache = session.firCachesFactory.createCache(::loadFunctionsByCallableId)
    private val propertyCache = session.firCachesFactory.createCache(::loadPropertiesByCallableId)

    override fun computePackageSetWithTopLevelCallables(): Set<String> {
        return packageSetWithTopLevelCallableDeclarations
    }

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        namesByPackageCache.getTopLevelCallableNamesInPackage(packageFqName)

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? {
        return namesByPackageCache.getTopLevelClassifierNamesInPackage(packageFqName)
    }

    private fun findAndDeserializeTypeAlias(classId: ClassId): Pair<FirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        val classLikeDeclaration = declarationProvider.getClassLikeDeclarationByClassId(classId)?.originalElement
        if (classLikeDeclaration is KtTypeAlias) {
            val symbol = FirTypeAliasSymbol(classId)
            val postProcessor: DeserializedTypeAliasPostProcessor = {
                val rootContext = StubBasedFirDeserializationContext.createRootContext(
                    moduleData,
                    StubBasedAnnotationDeserializer(session),
                    classId.packageFqName,
                    classId.relativeClassName,
                    classLikeDeclaration,
                    null, null, symbol
                )
                rootContext.memberDeserializer.loadTypeAlias(classLikeDeclaration, symbol)
            }
            return symbol to postProcessor
        }
        return null to null
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: StubBasedFirDeserializationContext? = null
    ): FirRegularClassSymbol? {
        val classLikeDeclaration = declarationProvider.getClassLikeDeclarationByClassId(classId)?.originalElement ?: return null
        val symbol = FirRegularClassSymbol(classId)
        if (classLikeDeclaration is KtClassOrObject) {
            deserializeClassToSymbol(
                classId,
                classLikeDeclaration,
                symbol,
                session,
                moduleData,
                StubBasedAnnotationDeserializer(session),
                kotlinScopeProvider,
                parentContext,
                JvmFromStubDecompilerSource(JvmClassName.byClassId(classId)),
                deserializeNestedClass = this::getClass,
            )
            return symbol
        }
        return null
    }

    private fun loadFunctionsByCallableId(callableId: CallableId): List<FirNamedFunctionSymbol> {
        val topLevelFunctions = declarationProvider.getTopLevelFunctions(callableId)
        val origins = if (topLevelFunctions.size > 1) mutableSetOf<KtNamedFunction>() else null
        return topLevelFunctions
            .mapNotNull { function ->
                val original = function.originalElement as? KtNamedFunction ?: return@mapNotNull null
                if (origins != null && !origins.add(original)) return@mapNotNull null
                val file = original.containingKtFile
                val virtualFile = file.virtualFile
                if (virtualFile.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION) return@mapNotNull null
                if (file.packageFqName.asString()
                        .replace(".", "/") + "/" + virtualFile.nameWithoutExtension in KotlinBuiltins
                ) return@mapNotNull null
                val symbol = FirNamedFunctionSymbol(callableId)
                val rootContext =
                    StubBasedFirDeserializationContext.createRootContext(session, moduleData, callableId, original, symbol)
                rootContext.memberDeserializer.loadFunction(original, null, session, symbol).symbol
            }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId): List<FirPropertySymbol> {
        val topLevelProperties = declarationProvider.getTopLevelProperties(callableId)
        val origins = if (topLevelProperties.size > 1) mutableSetOf<KtProperty>() else null
        return topLevelProperties
            .mapNotNull { property ->
                val original = property.originalElement as? KtProperty ?: return@mapNotNull null
                if (origins != null && !origins.add(original)) return@mapNotNull null
                val symbol = FirPropertySymbol(callableId)
                val rootContext =
                    StubBasedFirDeserializationContext.createRootContext(session, moduleData, callableId, original, symbol)
                rootContext.memberDeserializer.loadProperty(original, null, symbol).symbol
            }
    }

    private fun getClass(
        classId: ClassId,
        parentContext: StubBasedFirDeserializationContext? = null
    ): FirRegularClassSymbol? {
        return classCache.getValue(classId, parentContext)
    }

    private fun getTypeAlias(classId: ClassId): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null
        return typeAliasCache.getValue(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getCallables(callableId)
        destination += propertyCache.getCallables(callableId)
    }

    private fun <C : FirCallableSymbol<*>> FirCache<CallableId, List<C>, Nothing?>.getCallables(id: CallableId): List<C> {
        if (id.packageName.asString() !in packageSetWithTopLevelCallableDeclarations) return emptyList()
        if (!namesByPackageCache.mayHaveTopLevelCallable(id.packageName, id.callableName)) return emptyList()
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

    override fun getPackage(fqName: FqName): FqName? {
        return if (!namesByPackageCache.getTopLevelClassifierNamesInPackage(fqName)
                .isNullOrEmpty() || packageSetWithTopLevelCallableDeclarations.contains(fqName.asString())
        ) {
            fqName
        } else null
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!namesByPackageCache.mayHaveTopLevelClassifier(classId, mayHaveFunctionClass = false)) return null
        return getClass(classId) ?: getTypeAlias(classId)
    }
}
