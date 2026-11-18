/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirLazyValueWithContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.impl.pluginTypeResolverFactory
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirGeneratedClassDeclaredMemberScope private constructor(
    classId: ClassId,
    private val storage: FirGeneratedMemberDeclarationsStorage.CallableStorage,
    private val generationContext: MemberGenerationContext,
    private val nestedClassifierScope: FirNestedClassifierScope?
) : FirClassDeclaredMemberScope(classId) {
    companion object {
        fun create(
            useSiteSession: FirSession,
            classSymbol: FirClassSymbol<*>,
            regularDeclaredScope: FirClassDeclaredMemberScope?,
            scopeForGeneratedClass: Boolean
        ): FirGeneratedClassDeclaredMemberScope? {
            @OptIn(FirExtensionApiInternals::class)
            val typeResolver = useSiteSession.pluginTypeResolverFactory.getTypeResolver(classSymbol)

            val generationContext = MemberGenerationContext(classSymbol, regularDeclaredScope, typeResolver)

            /*
             * Extensions can modify source classes of the same session in which they are enabled
             * This implies the contract that if declaration-site session and use-site session
             *   differs for some class, generated declarations should be provided by extensions
             *   of declaration-site session
             */
            val storage = classSymbol.moduleData
                .session
                .generatedDeclarationsStorage
                .getCallableStorage(classSymbol, generationContext, scopeForGeneratedClass)
                ?: return null

            val nestedClassifierScope = runIf(scopeForGeneratedClass) {
                useSiteSession.nestedClassifierScope(classSymbol.fir)
            }

            return FirGeneratedClassDeclaredMemberScope(
                classSymbol.classId,
                storage,
                generationContext,
                nestedClassifierScope,
            )
        }
    }

    // ------------------------------------------ scope methods ------------------------------------------

    override fun getCallableNames(): Set<Name> {
        return storage.allCallableNames
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifierScope?.getClassifierNames() ?: emptySet()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        nestedClassifierScope?.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name !in getCallableNames()) return
        for (functionSymbol in storage.functionCache.getValue(name, generationContext)) {
            processor(functionSymbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (name !in getCallableNames()) return
        for (propertySymbol in storage.propertyCache.getValue(name, generationContext)) {
            processor(propertySymbol)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        for (constructorSymbol in storage.constructorCache.getValue(generationContext)) {
            processor(constructorSymbol)
        }
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirGeneratedClassDeclaredMemberScope? {
        return null
    }
}

class FirGeneratedClassNestedClassifierScope private constructor(
    useSiteSession: FirSession,
    klass: FirClass,
    private val storage: FirGeneratedMemberDeclarationsStorage.ClassifierStorage,
    private val generationContext: NestedClassGenerationContext,
) : FirNestedClassifierScope(klass, useSiteSession) {
    companion object {
        fun create(
            useSiteSession: FirSession,
            classSymbol: FirClassSymbol<*>,
            regularNestedClassifierScope: FirNestedClassifierScope?,
        ): FirGeneratedClassNestedClassifierScope? {
            @OptIn(FirExtensionApiInternals::class)
            val typeResolver = useSiteSession.pluginTypeResolverFactory.getTypeResolver(classSymbol)

            val generationContext = NestedClassGenerationContext(classSymbol, regularNestedClassifierScope, typeResolver)

            /*
             * Extensions can modify source classes of the same session in which they are enabled
             * This implies the contract that if declaration-site session and use-site session
             *   differs for some class, generated declarations should be provided by extensions
             *   of declaration-site session
             */
            val storage = classSymbol.moduleData
                .session
                .generatedDeclarationsStorage
                .getClassifierStorage(classSymbol, generationContext)
                ?: return null

            return FirGeneratedClassNestedClassifierScope(
                useSiteSession,
                classSymbol.fir,
                storage,
                generationContext,
            )
        }
    }

    override fun getNestedClassSymbol(name: Name): FirRegularClassSymbol? {
        return storage.classifiersCache.getValue(name, generationContext)
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun getClassifierNames(): Set<Name> {
        return storage.allClassifierNames
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirGeneratedClassNestedClassifierScope? {
        return null
    }
}

/**
 * A storage for caching compiler plugin-generated callables and classifiers. Centralized caching ensures the uniqueness of these
 * declarations, while avoiding injection of the declarations into the original FIR.
 *
 * @see CallableStorage
 * @see ClassifierStorage
 */
class FirGeneratedMemberDeclarationsStorage(private val session: FirSession) : FirSessionComponent {
    internal fun getCallableStorage(
        classSymbol: FirClassSymbol<*>,
        generationContext: MemberGenerationContext,
        scopeForGeneratedClass: Boolean
    ): CallableStorage? {
        val extensions = getExtensionsForClass(classSymbol)
        val extensionsByCallableName = extensions.flatGroupBy { it.getCallableNamesForClass(classSymbol, generationContext) }

        if (extensionsByCallableName.isEmpty() && !scopeForGeneratedClass) return null

        val context = CallableStorage.CacheContext(extensionsByCallableName)
        return callableStorageByClass.getValue(classSymbol, context)
    }

    internal fun getClassifierStorage(
        classSymbol: FirClassSymbol<*>,
        generationContext: NestedClassGenerationContext,
    ): ClassifierStorage? {
        val extensions = getExtensionsForClass(classSymbol)
        val extensionsByClassifierName = extensions.flatGroupBy { it.getNestedClassifiersNames(classSymbol, generationContext) }

        val companionObjectContext = CompanionObjectGenerationContext(generationContext.owner, generationContext.declaredScope)
        val companionObjectKeys =
            extensions.mapNotNullTo(mutableSetOf()) { it.needsCompanionObject(classSymbol, companionObjectContext) }

        if (extensionsByClassifierName.isEmpty() && companionObjectKeys.isEmpty()) return null

        val context = ClassifierStorage.CacheContext(extensionsByClassifierName, companionObjectKeys)
        return classifierStorageByClass.getValue(classSymbol, context)
    }

    private val callableStorageByClass: FirCache<FirClassSymbol<*>, CallableStorage, CallableStorage.CacheContext> =
        session.firCachesFactory.createCache { classSymbol, context -> CallableStorage(session, classSymbol, context) }

    private val classifierStorageByClass: FirCache<FirClassSymbol<*>, ClassifierStorage, ClassifierStorage.CacheContext> =
        session.firCachesFactory.createCache { classSymbol, context -> ClassifierStorage(session, classSymbol, context) }

    /**
     * A storage for compiler plugin-generated callables.
     *
     * The storage avoids caching the [MemberGenerationContext] that is used to generate declarations. This is crucial, as we would
     * otherwise cache the heavy member scope embedded into the context. Instead, the storage requires passing a generation context on each
     * cache access.
     *
     * @see FirGeneratedMemberDeclarationsStorage
     */
    internal class CallableStorage(
        private val session: FirSession,
        private val classSymbol: FirClassSymbol<*>,
        private val context: CacheContext,
    ) {
        class CacheContext(
            val extensionsByCallableName: Map<Name, List<FirDeclarationGenerationExtension>>,
        )

        val functionCache: FirCache<Name, List<FirNamedFunctionSymbol>, MemberGenerationContext> =
            session.firCachesFactory.createCache(::generateMemberFunctions)

        val propertyCache: FirCache<Name, List<FirVariableSymbol<*>>, MemberGenerationContext> =
            session.firCachesFactory.createCache(::generateMemberProperties)

        val constructorCache: FirLazyValueWithContext<List<FirConstructorSymbol>, MemberGenerationContext> =
            session.firCachesFactory.createLazyValueWithContext(::generateConstructors)

        val allCallableNames: Set<Name> = when {
            classSymbol.isCompilerGeneratedCompanion() -> context.extensionsByCallableName.keys + SpecialNames.INIT
            else -> context.extensionsByCallableName.keys
        }

        private fun generateMemberFunctions(name: Name, generationContext: MemberGenerationContext): List<FirNamedFunctionSymbol> {
            if (name == SpecialNames.INIT) return emptyList()
            return context.extensionsByCallableName[name].orEmpty()
                .flatMap { it.generateFunctions(CallableId(classSymbol.classId, name), generationContext) }
                .onEach { it.fir.validate() }
        }

        private fun generateMemberProperties(name: Name, generationContext: MemberGenerationContext): List<FirVariableSymbol<*>> {
            if (name == SpecialNames.INIT) return emptyList()
            val extensions = context.extensionsByCallableName[name] ?: return emptyList()
            return buildList {
                extensions.forEach { extension ->
                    val callableId = CallableId(classSymbol.classId, name)
                    @OptIn(UnsafePluginApi::class)
                    addAll(extension.generateFields(callableId, generationContext))
                    addAll(extension.generateProperties(callableId, generationContext))
                }
            }.onEach { it.fir.validate() }
        }

        private fun generateConstructors(generationContext: MemberGenerationContext): List<FirConstructorSymbol> {
            return if (classSymbol.isCompilerGeneratedCompanion()) {
                listOfNotNull(generateCompanionObjectConstructorForPlugins(classSymbol, session)?.symbol)
            } else {
                context.extensionsByCallableName[SpecialNames.INIT].orEmpty()
                    .flatMap { it.generateConstructors(generationContext) }
                    .onEach { it.fir.validate() }
            }
        }
    }

    /**
     * A storage for compiler plugin-generated classifiers.
     *
     * The storage avoids caching the [NestedClassGenerationContext] that is used to generate declarations. This is crucial, as we would
     * otherwise cache the heavy member scope embedded into the context. Instead, the storage requires passing a generation context on each
     * cache access.
     *
     * @see FirGeneratedMemberDeclarationsStorage
     */
    internal class ClassifierStorage(
        private val session: FirSession,
        private val classSymbol: FirClassSymbol<*>,
        private val context: CacheContext,
    ) {
        class CacheContext(
            val extensionsByClassifierName: Map<Name, List<FirDeclarationGenerationExtension>>,
            val companionObjectKeys: MutableSet<GeneratedDeclarationKey>,
        )

        val classifiersCache: FirCache<Name, FirRegularClassSymbol?, NestedClassGenerationContext> =
            session.firCachesFactory.createCache(::generateNestedClassifier)

        val allClassifierNames: Set<Name> = buildSet {
            if (needsCompanionObject) {
                add(DEFAULT_NAME_FOR_COMPANION_OBJECT)
            }
            addAll(context.extensionsByClassifierName.keys)
        }

        private val needsCompanionObject get() = context.companionObjectKeys.isNotEmpty()

        private fun generateNestedClassifier(name: Name, generationContext: NestedClassGenerationContext): FirRegularClassSymbol? {
            if (classSymbol is FirRegularClassSymbol) {
                val companion = classSymbol.companionObjectSymbol
                if (companion != null && companion.origin.generated && companion.classId.shortClassName == name) {
                    return companion
                }
            }

            val extensions = context.extensionsByClassifierName[name].orEmpty()
            val generatedClasses = extensions.mapNotNull { extension ->
                extension.generateNestedClassLikeDeclaration(classSymbol, name, generationContext)?.also { symbol ->
                    symbol.fir.ownerGenerator = extension
                    if (classSymbol.isLocal) {
                        symbol.fir.containingClassForLocalAttr = classSymbol.toLookupTag()
                    }
                }
            }

            if (
                name == DEFAULT_NAME_FOR_COMPANION_OBJECT && needsCompanionObject &&
                (classSymbol !is FirRegularClassSymbol || classSymbol.companionObjectSymbol == null)
            ) {
                if (generatedClasses.isNotEmpty()) {
                    error(
                        """
                        Some plugin(s) requested a companion object for ${classSymbol.classId} with 'needsCompanionObject()',
                        but other plugins generated it with 'generateNestedClassLikeDeclaration()':
                        ${generatedClasses.joinToString("\n") { it.fir.render() }}
                        """.trimIndent()
                    )
                }

                return generateCompanionObjectForPlugins(classSymbol, context.companionObjectKeys, session).symbol
            }

            val generatedClass = when (generatedClasses.size) {
                0 -> return null
                1 -> generatedClasses.first()
                else -> error(
                    """
                     Multiple plugins generated nested class with same name $name for class ${classSymbol.classId}:
                    ${generatedClasses.joinToString("\n") { it.fir.render() }}
                """.trimIndent()
                )
            }
            require(generatedClass is FirRegularClassSymbol) { "Only regular class are allowed as nested classes" }
            return generatedClass
        }
    }

    private fun getExtensionsForClass(classSymbol: FirClassSymbol<*>): List<FirDeclarationGenerationExtension> {
        require(session === classSymbol.moduleData.session) {
            "Class $classSymbol is declared in ${classSymbol.moduleData.session}, but generated storage for it taken from $session"
        }

        // Every plugin can add declarations to the generated companion, not only the plugins that return `true` from `needsCompanionObject`.
        // The companion then behaves like a companion object from source.
        return if (classSymbol.origin.generated && !classSymbol.isLocal && !classSymbol.isCompilerGeneratedCompanion()) {
            listOf(classSymbol.fir.ownerGenerator!!)
        } else {
            session.extensionService.declarationGenerators
        }
    }
}

private val FirSession.generatedDeclarationsStorage: FirGeneratedMemberDeclarationsStorage by FirSession.sessionComponentAccessor()

fun FirClassSymbol<*>.isCompilerGeneratedCompanion(): Boolean {
    return origin is FirDeclarationOrigin.Plugin.SharedCompanion
}

private fun generateCompanionObjectForPlugins(
    owner: FirClassSymbol<*>,
    keys: Set<GeneratedDeclarationKey>,
    session: FirSession,
): FirRegularClass {
    return buildRegularClass {
        symbol = FirRegularClassSymbol(owner.classId.createNestedClassId(DEFAULT_NAME_FOR_COMPANION_OBJECT))
        source = owner.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated.Default)

        // Shared companion object is generated with a phase of 'COMPANION_GENERATION' and resolved as normal by the compiler.
        // This ensures that other compiler plugin extensions (like 'FirSupertypeGenerationExtension') are able to operate on it.
        resolvePhase = FirResolvePhase.COMPANION_GENERATION
        moduleData = session.moduleData
        scopeProvider = session.kotlinScopeProvider
        @OptIn(FirExtensionApiInternals::class)
        origin = FirDeclarationOrigin.Plugin.SharedCompanion(keys)

        classKind = ClassKind.OBJECT
        val visibility = if (owner.isLocal) Visibilities.Local else Visibilities.Public
        status = FirResolvedDeclarationStatusImpl(
            visibility,
            Modality.FINAL,
            visibility.toEffectiveVisibility(owner, forClass = true)
        ).apply {
            isCompanion = true
            isExpect = owner.isExpect
        }
        name = DEFAULT_NAME_FOR_COMPANION_OBJECT
        superTypeRefs += session.builtinTypes.anyType
    }.apply {
        if (owner.isLocal) {
            containingClassForLocalAttr = owner.toLookupTag()
        }
    }
}

private object GeneratedCompanionConstructorKey : GeneratedDeclarationKey()

/**
 * Returns `null` if the superclass of [owner] has no constructor that can be called without arguments.
 * In this case, a plugin can generate the constructor in IR.
 */
private fun generateCompanionObjectConstructorForPlugins(owner: FirClassSymbol<*>, session: FirSession): FirConstructor? {
    val superTypeRef = session.builtinTypes.anyType

    // TODO ew...
    val superConstructorSymbol = superTypeRef.coneType.toRegularClassSymbol(session)
        ?.declaredMemberScope(session, memberRequiredPhase = null)
        ?.getDeclaredConstructors()
        ?.firstOrNull { constructor -> constructor.valueParameterSymbols.all { it.hasDefaultValue } }
        ?: return null

    return buildPrimaryConstructor {
        symbol = FirConstructorSymbol(owner.classId)
        source = owner.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated.Default)

        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = GeneratedCompanionConstructorKey.origin

        returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
        val visibility = Visibilities.Private
        status = FirResolvedDeclarationStatusImpl(
            visibility,
            FINAL,
            visibility.toEffectiveVisibility(owner, forClass = true)
        ).apply {
            isExpect = owner.isExpect
        }
        isLocal = owner.isLocal
        delegatedConstructor = buildDelegatedConstructorCall {
            constructedTypeRef = superTypeRef
            calleeReference = buildResolvedNamedReference {
                name = superConstructorSymbol.name
                resolvedSymbol = superConstructorSymbol
            }
            argumentList = FirEmptyArgumentList
            isThis = false
        }
    }.apply {
        containingClassForStaticMemberAttr = owner.toLookupTag()
    }
}
