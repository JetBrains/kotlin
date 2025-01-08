/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.getNotNullValueForNotNullContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.DeserializedContainerSourceProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.StubBasedAnnotationDeserializer
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.StubBasedFirDeserializationContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.StubBasedFirTypeDeserializer
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.loadStubByElement
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.java.deserialization.KotlinBuiltins
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

typealias DeserializedTypeAliasPostProcessor = (FirTypeAliasSymbol) -> Unit

/**
 * [LLKotlinStubBasedLibrarySymbolProvider] deserializes FIR symbols from existing stubs, retrieving them by [ClassId]/[CallableId] from a
 * [KotlinDeclarationProvider][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider].
 *
 * The symbol provider is currently only enabled in IDE mode. The Standalone mode uses
 * [JvmClassFileBasedSymbolProvider][org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider], which is also used by
 * the compiler.
 *
 * Because the symbol provider uses existing stubs, there is no need to keep a huge protobuf in memory, which would be the case for
 * metadata-based deserialization ([JvmClassFileBasedSymbolProvider][org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider]).
 * At the same time, there is no need to guess sources for FIR elements anymore, as they are set during deserialization.
 *
 * Like with [JvmClassFileBasedSymbolProvider][org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider], the resulting
 * deserialized FIR elements are already fully resolved.
 */
internal open class LLKotlinStubBasedLibrarySymbolProvider(
    session: LLFirSession,
    private val deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
    scope: GlobalSearchScope,
    // A workaround for KT-63718. It should be removed with KT-64236.
    isFallbackDependenciesProvider: Boolean,
) : LLKotlinSymbolProvider(session) {
    private val kotlinScopeProvider: FirKotlinScopeProvider get() = session.kotlinScopeProvider
    private val moduleData: LLFirModuleData get() = session.llFirModuleData

    final override val declarationProvider = session.project.createDeclarationProvider(
        scope,
        contextualModule = session.ktModule.takeIf { !isFallbackDependenciesProvider },
    )

    override val allowKotlinPackage: Boolean get() = true

    override val symbolNamesProvider: FirSymbolNamesProvider =
        LLFirKotlinSymbolNamesProvider.cached(session, declarationProvider, allowKotlinPackage)

    private val typeAliasCache: FirCache<ClassId, FirTypeAliasSymbol?, StubBasedFirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { classId, context -> findAndDeserializeTypeAlias(classId, context) },
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

    final override val packageProvider = session.project.createPackageProvider(scope)

    /**
     * Computes the origin for the declarations coming from [file].
     *
     * We assume that a stub Kotlin declaration might come only from Library or from BuiltIns.
     * We do the decision based upon the extension of the [file].
     *
     * This method is left open so the inheritors can provide more optimal/strict implementations.
     */
    protected open fun getDeclarationOriginFor(file: KtFile): FirDeclarationOrigin {
        val virtualFile = file.virtualFile

        return if (virtualFile.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
            FirDeclarationOrigin.BuiltIns
        } else {
            FirDeclarationOrigin.Library
        }
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId,
        context: StubBasedFirDeserializationContext?,
    ): Pair<FirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        val classLikeDeclaration =
            (context?.classLikeDeclaration ?: declarationProvider.getClassLikeDeclarationByClassId(classId))
        if (classLikeDeclaration is KtTypeAlias) {
            val symbol = FirTypeAliasSymbol(classId)
            val postProcessor: DeserializedTypeAliasPostProcessor = {
                val rootContext = context ?: StubBasedFirDeserializationContext.createRootContext(
                    moduleData,
                    StubBasedAnnotationDeserializer(session),
                    classId.packageFqName,
                    classId.relativeClassName,
                    classLikeDeclaration,
                    null, null, symbol,
                    initialOrigin = getDeclarationOriginFor(classLikeDeclaration.containingKtFile)
                )
                rootContext.memberDeserializer.loadTypeAlias(classLikeDeclaration, symbol)
            }
            return symbol to postProcessor
        }
        return null to null
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: StubBasedFirDeserializationContext?,
    ): FirRegularClassSymbol? {
        val classLikeDeclaration = parentContext?.classLikeDeclaration
            ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return null

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
                parentContext = parentContext,
                containerSource = deserializedContainerSourceProvider.getClassContainerSource(classId),
                deserializeNestedClass = this::getClass,
                initialOrigin = parentContext?.initialOrigin ?: getDeclarationOriginFor(classLikeDeclaration.containingKtFile)
            )

            val classStub = classLikeDeclaration.stub as? KotlinClassStub
                ?: loadStubByElement<KotlinClassOrObjectStub<KtClassOrObject>?, KtClassOrObject>(
                    classLikeDeclaration
                ) as? KotlinClassStub
            symbol.fir.isNewPlaceForBodyGeneration = classStub?.isClsStubCompiledToJvmDefaultImplementation()

            return symbol
        }
        return null
    }

    private fun loadFunctionsByCallableId(
        callableId: CallableId,
        foundFunctions: Collection<KtNamedFunction>?,
    ): List<FirNamedFunctionSymbol> {
        val topLevelFunctions = foundFunctions ?: declarationProvider.getTopLevelFunctions(callableId)

        return ArrayList<FirNamedFunctionSymbol>(topLevelFunctions.size).apply {
            for (function in topLevelFunctions) {
                val functionStub = function.stub as? KotlinFunctionStubImpl ?: loadStubByElement(function)
                val functionFile = function.containingKtFile
                val functionOrigin = getDeclarationOriginFor(functionFile)
                val containerSource =
                    deserializedContainerSourceProvider.getFacadeContainerSource(functionFile, functionStub?.origin, functionOrigin)

                if (!functionOrigin.isBuiltIns &&
                    containerSource is FacadeClassSource &&
                    containerSource.className.internalName in KotlinBuiltins
                ) {
                    continue
                }

                val symbol = FirNamedFunctionSymbol(callableId)
                val rootContext = StubBasedFirDeserializationContext
                    .createRootContext(session, moduleData, callableId, function, symbol, functionOrigin, containerSource)

                add(rootContext.memberDeserializer.loadFunction(function, null, session, symbol).symbol)
            }
        }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId, foundProperties: Collection<KtProperty>?): List<FirPropertySymbol> {
        val topLevelProperties = foundProperties ?: declarationProvider.getTopLevelProperties(callableId)

        return buildList {
            for (property in topLevelProperties) {
                val propertyStub = property.stub as? KotlinPropertyStubImpl ?: loadStubByElement(property)
                val propertyFile = property.containingKtFile
                val propertyOrigin = getDeclarationOriginFor(propertyFile)
                val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                    propertyFile,
                    propertyStub?.origin,
                    propertyOrigin,
                )

                val symbol = FirPropertySymbol(callableId)
                val rootContext = StubBasedFirDeserializationContext
                    .createRootContext(session, moduleData, callableId, property, symbol, propertyOrigin, containerSource)

                add(rootContext.memberDeserializer.loadProperty(property, null, symbol).symbol)
            }
        }
    }

    private fun getClass(classId: ClassId, parentContext: StubBasedFirDeserializationContext? = null): FirRegularClassSymbol? =
        if (parentContext?.classLikeDeclaration != null) {
            classCache.getNotNullValueForNotNullContext(classId, parentContext)
        } else {
            classCache.getValue(classId, parentContext)
        }

    private fun getTypeAlias(classId: ClassId, context: StubBasedFirDeserializationContext? = null): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null
        return typeAliasCache.getValue(classId, context)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getCallablesWithoutContext(callableId)
        destination += propertyCache.getCallablesWithoutContext(callableId)
    }

    private fun <C : FirCallableSymbol<*>, CONTEXT> FirCache<CallableId, List<C>, CONTEXT?>.getCallablesWithoutContext(
        id: CallableId,
    ): List<C> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(id.packageName, id.callableName)) return emptyList()
        return getValue(id, null)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<FirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<KtCallableDeclaration>,
    ) {
        callables.filterIsInstance<KtNamedFunction>().ifNotEmpty {
            destination += functionCache.getValue(callableId, this)
        }

        callables.filterIsInstance<KtProperty>().ifNotEmpty {
            destination += propertyCache.getValue(callableId, this)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.getCallablesWithoutContext(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<FirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<KtNamedFunction>,
    ) {
        destination += functionCache.getValue(callableId, functions)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.getCallablesWithoutContext(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<FirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<KtProperty>,
    ) {
        destination += propertyCache.getValue(callableId, properties)
    }

    override fun hasPackage(fqName: FqName): Boolean =
        packageProvider.doesKotlinOnlyPackageExist(fqName)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        getCachedClassLikeSymbol(classId)?.let { return it }

        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        classId.takeIf(ClassId::isNestedClass)?.outermostClassId?.let { outermostClassId ->
            // We have to load root declaration to initialize nested classes correctly
            getClassLikeSymbolByClassId(outermostClassId)

            // Nested declarations already loaded
            getCachedClassLikeSymbol(classId)?.let { return it }
        }

        return getClass(classId) ?: getTypeAlias(classId)
    }

    private fun getCachedClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? {
        return classCache.getValueIfComputed(classId) ?: typeAliasCache.getValueIfComputed(classId)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
        val cache = if (classLikeDeclaration is KtClassOrObject) classCache else typeAliasCache
        cache.getValueIfComputed(classId)?.let { return it }

        val topmostClassLikeDeclaration = classLikeDeclaration.takeIf {
            classId.isNestedClass
        }?.getTopmostParentOfType<KtClassLikeDeclaration>()

        val outermostClassId = topmostClassLikeDeclaration?.getClassId()
        if (outermostClassId != null) {
            // We have to load root declaration to initialize nested classes correctly
            getClassLikeSymbolByClassId(outermostClassId, topmostClassLikeDeclaration)

            // Nested declarations already loaded
            cache.getValueIfComputed(classId)?.let { return it }
        }

        val annotationDeserializer = StubBasedAnnotationDeserializer(session)
        val classOrigin = getDeclarationOriginFor(classLikeDeclaration.containingKtFile)
        val deserializationContext = StubBasedFirDeserializationContext(
            moduleData,
            classId.packageFqName,
            classId.relativeClassName,
            StubBasedFirTypeDeserializer(
                moduleData,
                annotationDeserializer,
                parent = null,
                containingSymbol = null,
                owner = null,
                classOrigin
            ),
            annotationDeserializer,
            containerSource = null,
            outerClassSymbol = null,
            outerTypeParameters = emptyList(),
            classOrigin,
            classLikeDeclaration,
        )

        return cache.getNotNullValueForNotNullContext(classId, deserializationContext)
    }

    fun getTopLevelCallableSymbol(
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: KtCallableDeclaration,
    ): FirCallableSymbol<*>? {
        //possible overloads spoils here
        //we can't use only this callable instead of index access to fill the cache
        //names check is redundant though as we already have existing callable in scope
        val callableId = CallableId(packageFqName, shortName)
        val callableSymbols = when (callableDeclaration) {
            is KtNamedFunction -> functionCache.getValue(callableId)
            is KtProperty -> propertyCache.getValue(callableId)
            else -> null
        }
        return callableSymbols?.singleOrNull { it.fir.realPsi == callableDeclaration }
    }
}
