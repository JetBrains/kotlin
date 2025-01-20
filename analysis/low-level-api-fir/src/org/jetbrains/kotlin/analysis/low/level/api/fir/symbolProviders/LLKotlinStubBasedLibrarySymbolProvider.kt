/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.getNotNullValueForNotNullContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.caches.LLAmbiguousClassLikeSymbolCache
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.java.deserialization.KotlinBuiltins
import org.jetbrains.kotlin.fir.moduleData
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
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

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
) : LLKotlinSymbolProvider(session) {
    private val kotlinScopeProvider: FirKotlinScopeProvider get() = session.kotlinScopeProvider
    private val moduleData: LLFirModuleData get() = session.llFirModuleData

    final override val declarationProvider = session.project.createDeclarationProvider(
        scope,
        contextualModule = session.ktModule,
    )

    override val allowKotlinPackage: Boolean get() = true

    override val symbolNamesProvider: FirSymbolNamesProvider =
        LLFirKotlinSymbolNamesProvider.cached(session, declarationProvider, allowKotlinPackage)

    private val typeAliasCache: FirCache<ClassId, FirTypeAliasSymbol?, StubBasedFirDeserializationContext?> =
        createTypeAliasCache { classId, context -> findAndDeserializeTypeAlias(classId, context) }

    private val ambiguousTypeAliasCache = LLAmbiguousClassLikeSymbolCache(
        this,
        scope,
        createTypeAliasCache { declaration, context ->
            val classId = declaration.getClassId() ?: return@createTypeAliasCache Pair(null, null)
            findAndDeserializeTypeAlias(classId, context)
        },
    )

    private inline fun <K : Any> createTypeAliasCache(
        crossinline deserialize: (K, StubBasedFirDeserializationContext?) -> Pair<FirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?>,
    ): FirCache<K, FirTypeAliasSymbol?, StubBasedFirDeserializationContext?> =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = { key, context ->
                deserialize(key, context)
            },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            },
        )

    private val classCache: FirCache<ClassId, FirRegularClassSymbol?, StubBasedFirDeserializationContext?> =
        session.firCachesFactory.createCache(
            createValue = { classId, context -> findAndDeserializeClass(classId, context) }
        )

    private val ambiguousClassCache =
        LLAmbiguousClassLikeSymbolCache(this, scope) { declaration, context: StubBasedFirDeserializationContext? ->
            val classId = declaration.getClassId() ?: return@LLAmbiguousClassLikeSymbolCache null
            findAndDeserializeClass(classId, context)
        }

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
                rootContext.memberDeserializer.loadTypeAlias(classLikeDeclaration, symbol, kotlinScopeProvider)
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
                deserializeNestedClass = this::getNestedClass,
                initialOrigin = parentContext?.initialOrigin ?: getDeclarationOriginFor(classLikeDeclaration.containingKtFile)
            )

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
                val symbol = loadFunction(
                    function = function,
                    callableId = callableId,
                    functionOrigin = getDeclarationOriginFor(function.containingKtFile),
                    deserializedContainerSourceProvider = deserializedContainerSourceProvider,
                    session = session,
                ) ?: continue
                add(symbol)
            }
        }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId, foundProperties: Collection<KtProperty>?): List<FirPropertySymbol> {
        val topLevelProperties = foundProperties ?: declarationProvider.getTopLevelProperties(callableId)

        return ArrayList<FirPropertySymbol>(topLevelProperties.size).apply {
            for (property in topLevelProperties) {
                val symbol = loadProperty(
                    property = property,
                    callableId = callableId,
                    propertyOrigin = getDeclarationOriginFor(property.containingKtFile),
                    deserializedContainerSourceProvider = deserializedContainerSourceProvider,
                    session = session,
                ) ?: continue
                add(symbol)
            }
        }
    }

    private fun getNestedClass(
        classId: ClassId,
        declaration: KtClassLikeDeclaration,
        parentContext: StubBasedFirDeserializationContext,
    ): FirRegularClassSymbol? {
        requireWithAttachment(
            parentContext.classLikeDeclaration != null,
            { "The context should have a class-like declaration when deserializing nested classes." },
        ) {
            withPsiEntry("declaration", declaration, session.llFirModuleData.ktModule)
        }

        return classCache.getNotNullValueForNotNullContext(classId, parentContext)
            ?.takeIf { it.hasPsi(declaration) }
            ?: ambiguousClassCache.getSymbol(declaration, parentContext)
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
            // We have to load the root declaration to initialize nested classes correctly.
            getClassLikeSymbolByClassId(outermostClassId)

            // Nested declarations are already loaded.
            getCachedClassLikeSymbol(classId)?.let { return it }
        }

        return getClass(classId) ?: getTypeAlias(classId)
    }

    private fun getCachedClassLikeSymbol(classId: ClassId): FirClassLikeSymbol<*>? {
        return classCache.getValueIfComputed(classId)
            ?: typeAliasCache.getValueIfComputed(classId)
    }

    private fun getClass(classId: ClassId): FirRegularClassSymbol? {
        return classCache.getValue(classId, context = null)
    }

    private fun getTypeAlias(classId: ClassId): FirTypeAliasSymbol? {
        if (!classId.relativeClassName.isOneSegmentFQN()) return null
        return typeAliasCache.getValue(classId, context = null)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
        val cache = if (classLikeDeclaration is KtClassOrObject) classCache else typeAliasCache
        cache.getValueIfComputed(classId)?.let { return it }

        val topmostClassLikeDeclaration = classLikeDeclaration.takeIf {
            classId.isNestedClass
        }?.getTopmostParentOfType<KtClassLikeDeclaration>()

        val outermostClassId = topmostClassLikeDeclaration?.getClassId()
        if (outermostClassId != null) {
            // We have to load the root declaration to initialize nested classes correctly.
            getClassLikeSymbolByClassId(outermostClassId, topmostClassLikeDeclaration)

            // Nested declarations are already loaded. In contrast to `getClassLikeSymbolByPsi`, we want to specifically load by `classId`
            // here, so there's no need to access the ambiguity cache.
            cache.getValueIfComputed(classId)?.let { return it }
        }

        return cache.getNotNullValueForNotNullContext(classId, createClassLikeDeserializationContext(classId, classLikeDeclaration))
    }

    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): FirClassLikeSymbol<*>? {
        if (declaration !is KtClassLikeDeclaration) return null

        val cache = if (declaration is KtClassOrObject) classCache else typeAliasCache
        cache.getValueIfComputed(classId)
            ?.takeIf { it.hasPsi(declaration) }
            ?.let { return it }

        val ambiguityCache = if (declaration is KtClassOrObject) ambiguousClassCache else ambiguousTypeAliasCache

        val topmostClassLikeDeclaration = declaration.takeIf {
            classId.isNestedClass
        }?.getTopmostParentOfType<KtClassLikeDeclaration>()

        val outermostClassId = topmostClassLikeDeclaration?.getClassId()
        if (outermostClassId != null) {
            // We have to load the root declaration to initialize nested classes correctly.
            getClassLikeSymbolByPsi(outermostClassId, topmostClassLikeDeclaration)

            // Nested declarations are already loaded.
            val result = cache.getValueIfComputed(classId)
                ?.takeIf { it.hasPsi(declaration) }
                ?: ambiguityCache.getSymbolIfCached(declaration)

            result?.let { return it }
        }

        return ambiguityCache.getClassLikeSymbolByPsi<KtClassLikeDeclaration>(classId, declaration) { declaration ->
            createClassLikeDeserializationContext(classId, declaration)
        }
    }

    private fun createClassLikeDeserializationContext(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration,
    ): StubBasedFirDeserializationContext {
        val annotationDeserializer = StubBasedAnnotationDeserializer(session)
        val classOrigin = getDeclarationOriginFor(classLikeDeclaration.containingKtFile)
        return StubBasedFirDeserializationContext(
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

    companion object {
        fun loadProperty(
            property: KtProperty,
            callableId: CallableId,
            propertyOrigin: FirDeclarationOrigin,
            deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
            session: FirSession,
        ): FirPropertySymbol? {
            val propertyStub = property.stub as? KotlinPropertyStubImpl ?: loadStubByElement(property)
            val propertyFile = property.containingKtFile
            val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                file = propertyFile,
                stubOrigin = propertyStub?.origin,
                declarationOrigin = propertyOrigin,
            )

            val symbol = FirRegularPropertySymbol(callableId)
            val rootContext = StubBasedFirDeserializationContext.createRootContext(
                session = session,
                moduleData = session.moduleData,
                callableId = callableId,
                parameterListOwner = property,
                symbol = symbol,
                initialOrigin = propertyOrigin,
                containerSource = containerSource,
            )

            return rootContext.memberDeserializer.loadProperty(
                property = property,
                classSymbol = null,
                existingSymbol = symbol,
            ).symbol
        }

        fun loadFunction(
            function: KtNamedFunction,
            callableId: CallableId,
            functionOrigin: FirDeclarationOrigin,
            deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
            session: FirSession,
        ): FirNamedFunctionSymbol? {
            val functionStub = function.stub as? KotlinFunctionStubImpl ?: loadStubByElement(function)
            val functionFile = function.containingKtFile
            val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                file = functionFile,
                stubOrigin = functionStub?.origin,
                declarationOrigin = functionOrigin,
            )

            if (!functionOrigin.isBuiltIns &&
                containerSource is FacadeClassSource &&
                containerSource.className.internalName in KotlinBuiltins
            ) {
                return null
            }

            val symbol = FirNamedFunctionSymbol(callableId)
            val rootContext = StubBasedFirDeserializationContext.createRootContext(
                session = session,
                moduleData = session.moduleData,
                callableId = callableId,
                parameterListOwner = function,
                symbol = symbol,
                initialOrigin = functionOrigin,
                containerSource = containerSource,
            )

            return rootContext.memberDeserializer.loadFunction(
                function = function,
                classSymbol = null,
                session = session,
                existingSymbol = symbol,
            ).symbol
        }
    }
}
