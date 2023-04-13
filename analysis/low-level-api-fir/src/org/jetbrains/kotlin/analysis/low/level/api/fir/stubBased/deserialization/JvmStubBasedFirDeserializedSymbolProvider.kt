/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import java.nio.file.Path
import java.nio.file.Paths

typealias DeserializedClassPostProcessor = (FirRegularClassSymbol) -> Unit

typealias DeserializedTypeAliasPostProcessor = (FirTypeAliasSymbol) -> Unit

class JvmStubBasedFirDeserializedSymbolProvider(
    session: FirSession,
    private val moduleDataProvider: ModuleDataProvider,
    private val kotlinScopeProvider: FirKotlinScopeProvider,
    private val packagePartProvider: PackagePartProvider,
    private val javaFacade: FirJavaFacade,
    private val declarationProvider: KotlinDeclarationProvider
) : FirSymbolProvider(session) {
    private val packageNamesForNonClassDeclarations: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        packagePartProvider.computePackageSetWithNonClassDeclarations()
    }

    private val classLikeNamesByPackage: FirCache<FqName, Set<Name>, Nothing?> =
        session.firCachesFactory.createCache { fqName: FqName ->
            declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(fqName)
        }

    private val allCallableNamesByPackage: FirCache<FqName, Set<Name>, Nothing?> =
        session.firCachesFactory.createCache { fqName: FqName ->
            declarationProvider.getTopLevelCallableNamesInPackage(fqName)
        }

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

    override fun computePackageSetWithTopLevelCallables(): Set<String> {
        return packageNamesForNonClassDeclarations
    }

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String> {
        return classLikeNamesByPackage.getValue(packageFqName).map { it.asString() }.toSet()
    }

    private fun KtDeclaration.containingLibrary(): Path? {
        PsiUtil.getVirtualFile(this)?.path?.split("!/")?.firstOrNull()?.let {
            return Paths.get(it).normalize()
        } ?: return null
    }

    private fun findAndDeserializeTypeAlias(classId: ClassId): Pair<FirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        val classLikeDeclaration = declarationProvider.getClassLikeDeclarationByClassId(classId)
        if (classLikeDeclaration is KtTypeAlias) {
            val moduleData = moduleDataProvider.getModuleData(classLikeDeclaration.containingLibrary()) ?: return null to null
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
    ): Pair<FirRegularClassSymbol?, DeserializedClassPostProcessor?> {
        val classLikeDeclaration = declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null to null
        val symbol = FirRegularClassSymbol(classId)
        if (classLikeDeclaration is KtClassOrObject) {
            val moduleData = moduleDataProvider.getModuleData(classLikeDeclaration.containingLibrary()) ?: return null to null
            deserializeClassToSymbol(
                classId,
                classLikeDeclaration,
                symbol,
                session,
                moduleData,
                StubBasedAnnotationDeserializer(session),
                kotlinScopeProvider,
                parentContext,
                JvmFromStubDecompilerSource(classId.packageFqName),
                deserializeNestedClass = this::getClass,
            )
            return symbol to { loadAnnotationsFromFile() }
        }
        return null to null
    }

    private fun loadAnnotationsFromFile(
        //symbol: FirRegularClassSymbol
    ) {
        //todo annotations
        //val annotations = mutableListOf<FirAnnotation>()
        //symbol.fir.replaceAnnotations(annotations.toMutableOrEmpty())
        //symbol.fir.replaceDeprecationsProvider(symbol.fir.getDeprecationsProvider(session))
    }

    private fun loadFunctionsByCallableId(callableId: CallableId): List<FirNamedFunctionSymbol> {
        return declarationProvider.getTopLevelFunctions(callableId).mapNotNull { function ->
            val moduleData = moduleDataProvider.getModuleData(function.containingLibrary()) ?: return@mapNotNull null
            val symbol = FirNamedFunctionSymbol(callableId)
            val createRootContext = StubBasedFirDeserializationContext.createRootContext(session, moduleData, callableId, function, symbol)
            createRootContext.memberDeserializer.loadFunction(function, null, session, symbol).symbol
        }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId): List<FirPropertySymbol> {
        return declarationProvider.getTopLevelProperties(callableId).mapNotNull { property ->
            val moduleData = moduleDataProvider.getModuleData(property.containingLibrary()) ?: return@mapNotNull null
            val symbol = FirPropertySymbol(callableId)
            val createRootContext = StubBasedFirDeserializationContext.createRootContext(session, moduleData, callableId, property, symbol)
            createRootContext.memberDeserializer.loadProperty(property, null, symbol).symbol
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
        if (id.packageName.asString() !in packageNamesForNonClassDeclarations) return emptyList()
        if (id.callableName !in allCallableNamesByPackage.getValue(id.packageName)) return emptyList()
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
        return javaFacade.getPackage(fqName)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!classLikeNamesByPackage.getValue(classId.packageFqName).contains(classId.shortClassName)) return null
        return getClass(classId) ?: getTypeAlias(classId)
    }
}
