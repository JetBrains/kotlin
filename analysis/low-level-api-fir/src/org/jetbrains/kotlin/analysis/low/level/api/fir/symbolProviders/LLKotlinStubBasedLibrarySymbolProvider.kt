/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.declarations.createDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.utils.errors.withPsiEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache
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
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
 * The symbol provider is currently only enabled in IDE mode. The Standalone mode uses [LLJvmClassFileBasedSymbolProvider] whose base class
 * [JvmClassFileBasedSymbolProvider][org.jetbrains.kotlin.fir.java.deserialization.JvmClassFileBasedSymbolProvider] is also used by the
 * compiler.
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

    private val module: KaModule
        get() = moduleData.ktModule

    final override val declarationProvider = session.project.createDeclarationProvider(
        scope,
        contextualModule = session.ktModule,
    )

    override val allowKotlinPackage: Boolean get() = true

    override val symbolNamesProvider: FirSymbolNamesProvider =
        LLFirKotlinSymbolNamesProvider.cached(session, declarationProvider, allowKotlinPackage)

    private val typeAliasCache = LLPsiAwareClassLikeSymbolCache(
        createTypeAliasCache(::findAndDeserializeTypeAlias),
        createTypeAliasCache { declaration: KtClassLikeDeclaration, context ->
            val classId = declaration.getClassId() ?: return@createTypeAliasCache Pair(null, null)
            findAndDeserializeTypeAlias(classId, declaration, context)
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

    private val classCache = LLPsiAwareClassLikeSymbolCache(
        session,
        ::findAndDeserializeClass,
    ) { declaration: KtClassLikeDeclaration, context ->
        val classId = declaration.getClassId() ?: return@LLPsiAwareClassLikeSymbolCache null
        findAndDeserializeClass(classId, declaration, context)
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
        val declaration = context?.classLikeDeclaration
            ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return Pair(null, null)

        return findAndDeserializeTypeAlias(classId, declaration, context)
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId,
        declaration: KtClassLikeDeclaration,
        context: StubBasedFirDeserializationContext?,
    ): Pair<FirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        if (declaration !is KtTypeAlias) return Pair(null, null)

        checkDeclarationAndContextConsistency(declaration, context)

        val symbol = FirTypeAliasSymbol(classId)
        val postProcessor: DeserializedTypeAliasPostProcessor = {
            val rootContext = context ?: StubBasedFirDeserializationContext.createRootContext(
                moduleData,
                StubBasedAnnotationDeserializer(session),
                classId.packageFqName,
                classId.relativeClassName,
                declaration,
                null, null, symbol,
                initialOrigin = getDeclarationOriginFor(declaration.containingKtFile)
            )
            rootContext.memberDeserializer.loadTypeAlias(declaration, symbol, kotlinScopeProvider)
        }
        return symbol to postProcessor
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: StubBasedFirDeserializationContext?,
    ): FirRegularClassSymbol? {
        val declaration = parentContext?.classLikeDeclaration
            ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return null

        return findAndDeserializeClass(classId, declaration, parentContext)
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        declaration: KtClassLikeDeclaration,
        parentContext: StubBasedFirDeserializationContext?,
    ): FirRegularClassSymbol? {
        if (declaration !is KtClassOrObject) return null

        checkDeclarationAndContextConsistency(declaration, parentContext)

        val symbol = FirRegularClassSymbol(classId)
        deserializeClassToSymbol(
            classId,
            declaration,
            symbol,
            session,
            moduleData,
            StubBasedAnnotationDeserializer(session),
            kotlinScopeProvider,
            parentContext = parentContext,
            containerSource = deserializedContainerSourceProvider.getClassContainerSource(classId),
            deserializeNestedClassLikeDeclaration = this::getNestedClassLikeDeclaration,
            initialOrigin = parentContext?.initialOrigin ?: getDeclarationOriginFor(declaration.containingKtFile)
        )

        return symbol
    }

    private fun checkDeclarationAndContextConsistency(
        declaration: KtClassLikeDeclaration,
        context: StubBasedFirDeserializationContext?,
    ) {
        requireWithAttachment(
            context?.classLikeDeclaration == null || declaration === context.classLikeDeclaration,
            { "The declaration to deserialize should be the same as the context's declaration." },
        ) {
            withPsiEntry("declaration", declaration, module)
            withPsiEntry("context.classLikeDeclaration", context?.classLikeDeclaration, module)
        }
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
                )
                add(symbol)
            }
        }
    }

    private fun getNestedClassLikeDeclaration(
        classId: ClassId,
        declaration: KtClassLikeDeclaration,
        parentContext: StubBasedFirDeserializationContext,
    ): FirClassLikeSymbol<*>? {
        requireWithAttachment(
            parentContext.classLikeDeclaration != null,
            { "The context should have a class-like declaration when deserializing nested classes or type aliases." },
        ) {
            withPsiEntry("declaration", declaration, module)
        }

        // We can assume that the outer class is in the scope since we're deserializing it with this symbol provider. Since the nested class or typealias
        // is in the same file as its outer class, it's definitely also in the scope of the symbol provider.
        val cache = if (declaration is KtClassOrObject) {
            classCache
        } else {
            require(declaration is KtTypeAlias)
            typeAliasCache
        }
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return cache.getSymbolByPsi(classId, declaration, parentContext)
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
        return classCache.getCachedSymbolByClassId(classId)
            ?: typeAliasCache.getCachedSymbolByClassId(classId)
    }

    private fun getClass(classId: ClassId): FirRegularClassSymbol? {
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return classCache.getSymbolByClassId(classId, context = null)
    }

    private fun getTypeAlias(classId: ClassId): FirTypeAliasSymbol? {
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return typeAliasCache.getSymbolByClassId(classId, context = null)
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeSymbol<*>? {
        val cache = if (classLikeDeclaration is KtClassOrObject) classCache else typeAliasCache
        cache.getCachedSymbolByClassId(classId)?.let { return it }

        classLikeDeclaration.runIfNested(classId) { topLevelDeclaration, topLevelClassId ->
            // We have to load the root declaration to initialize nested classes correctly.
            getClassLikeSymbolByClassId(topLevelClassId, topLevelDeclaration)

            // Nested declarations are already loaded. In contrast to `getClassLikeSymbolByPsi`, we want to specifically load by `classId`
            // here, so there's no need to access the ambiguity cache.
            cache.getCachedSymbolByClassId(classId)?.let { return it }
        }

        return cache.getSymbolByClassId(classId, createClassLikeDeserializationContext(classId, classLikeDeclaration))
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): FirClassLikeSymbol<*>? {
        if (declaration !is KtClassLikeDeclaration) return null

        val cache = if (declaration is KtClassOrObject) classCache else typeAliasCache
        cache.getCachedSymbolByPsi(classId, declaration)?.let { return it }

        declaration.runIfNested(classId) { topLevelDeclaration, topLevelClassId ->
            // We have to load the root declaration to initialize nested classes correctly.
            getClassLikeSymbolByPsi(topLevelClassId, topLevelDeclaration)

            // Nested declarations are already loaded.
            cache.getCachedSymbolByPsi(classId, declaration)?.let { return it }
        }

        return cache.getSymbolByPsi(classId, declaration, createClassLikeDeserializationContext(classId, declaration))
    }

    private inline fun KtClassLikeDeclaration.runIfNested(
        classId: ClassId,
        action: (KtClassLikeDeclaration, ClassId) -> Unit,
    ) {
        if (!classId.isNestedClass) return

        val topLevelDeclaration = getTopmostParentOfType<KtClassLikeDeclaration>() ?: return
        val topLevelClassId = topLevelDeclaration.getClassId() ?: return

        action(topLevelDeclaration, topLevelClassId)
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
        ): FirPropertySymbol {
            val propertyStub: KotlinPropertyStubImpl = property.compiledStub
            val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                file = property.containingKtFile,
                stubOrigin = propertyStub.origin,
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
            val functionStub: KotlinFunctionStubImpl = function.compiledStub
            val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                file = function.containingKtFile,
                stubOrigin = functionStub.origin,
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
