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
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.FirLazyValueWithContext
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildDelegatedConstructorCall
import org.jetbrains.kotlin.fir.extensions.*
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
            val generationContext = MemberGenerationContext(classSymbol, regularDeclaredScope)

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
            val generationContext = NestedClassGenerationContext(classSymbol, regularNestedClassifierScope)

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
    private val cachesFactory = session.firCachesFactory

    internal fun getCallableStorage(
        classSymbol: FirClassSymbol<*>,
        generationContext: MemberGenerationContext,
        scopeForGeneratedClass: Boolean
    ): CallableStorage? {
        val extensionsByCallableName = groupExtensionsByName(classSymbol) { getCallableNamesForClass(it, generationContext) }
        if (extensionsByCallableName.isEmpty() && !scopeForGeneratedClass) return null
        return callableStorageByClass.getValue(classSymbol, extensionsByCallableName)
    }

    internal fun getClassifierStorage(
        classSymbol: FirClassSymbol<*>,
        generationContext: NestedClassGenerationContext,
    ): ClassifierStorage? {
        val extensionsByClassifierName = groupExtensionsByName(classSymbol) { getNestedClassifiersNames(it, generationContext) }
        val extensionsNeedingCompanionObject =
            getExtensionsForClass(classSymbol).filter { it.needsCompanionObject(classSymbol, generationContext) }
        if (extensionsByClassifierName.isEmpty() && extensionsNeedingCompanionObject.isEmpty()) return null
        return classifierStorageByClass.getValue(
            classSymbol,
            ClassifierStorage.Context(extensionsByClassifierName, extensionsNeedingCompanionObject)
        )
    }

    private typealias ExtensionsByName = Map<Name, List<FirDeclarationGenerationExtension>>

    private val callableStorageByClass: FirCache<FirClassSymbol<*>, CallableStorage, ExtensionsByName> =
        cachesFactory.createCache { classSymbol, extensionsMap ->
            CallableStorage(cachesFactory, session, classSymbol, extensionsMap)
        }

    private val classifierStorageByClass: FirCache<FirClassSymbol<*>, ClassifierStorage, ClassifierStorage.Context> =
        cachesFactory.createCache { classSymbol, context ->
            ClassifierStorage(
                cachesFactory,
                session,
                classSymbol,
                context.extensionsByClassifierName,
                context.extensionsNeedingCompanionObject
            )
        }

    object GeneratedCompanionKey : GeneratedDeclarationKey()

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
        cachesFactory: FirCachesFactory,
        private val session: FirSession,
        val classSymbol: FirClassSymbol<*>,
        private val extensionsByCallableName: Map<Name, List<FirDeclarationGenerationExtension>>
    ) {
        val functionCache: FirCache<Name, List<FirNamedFunctionSymbol>, MemberGenerationContext> =
            cachesFactory.createCache { name, context -> generateMemberFunctions(name, context) }

        val propertyCache: FirCache<Name, List<FirVariableSymbol<*>>, MemberGenerationContext> =
            cachesFactory.createCache { name, context -> generateMemberProperties(name, context) }

        val constructorCache: FirLazyValueWithContext<List<FirConstructorSymbol>, MemberGenerationContext> =
            cachesFactory.createLazyValueWithContext { context -> generateConstructors(context) }

        val allCallableNames: Set<Name>
            get() = if (isSharedGeneratedCompanion) extensionsByCallableName.keys + SpecialNames.INIT else extensionsByCallableName.keys

        private val isSharedGeneratedCompanion = classSymbol.origin == GeneratedCompanionKey.origin

        private fun generateMemberFunctions(name: Name, generationContext: MemberGenerationContext): List<FirNamedFunctionSymbol> {
            if (name == SpecialNames.INIT) return emptyList()
            return extensionsByCallableName[name].orEmpty()
                .flatMap { it.generateFunctions(CallableId(classSymbol.classId, name), generationContext) }
                .onEach { it.fir.validate() }
        }

        private fun generateMemberProperties(name: Name, generationContext: MemberGenerationContext): List<FirVariableSymbol<*>> {
            if (name == SpecialNames.INIT) return emptyList()
            val extensions = extensionsByCallableName[name] ?: return emptyList()
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
            return extensionsByCallableName[SpecialNames.INIT].orEmpty()
                .flatMap { it.generateConstructors(generationContext) }
                .onEach { it.fir.validate() }
                .ifEmpty { if (isSharedGeneratedCompanion) listOf(generateCompanionObjectConstructorForPlugins(classSymbol).symbol) else emptyList() }
        }

        private fun generateCompanionObjectConstructorForPlugins(owner: FirClassSymbol<*>): FirConstructor = buildPrimaryConstructor {
            symbol = FirConstructorSymbol(owner.classId)
            source = owner.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated.Default)

            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = GeneratedCompanionKey.origin

            returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
            val visibility = Visibilities.Private
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(owner, forClass = true)
            ).apply {
                isExpect = owner.isExpect
            }
            isLocal = owner.isLocal
            delegatedConstructor = buildDelegatedConstructorCall {
                constructedTypeRef = session.builtinTypes.anyType
                val superConstructorSymbol = session.builtinTypes.anyType.coneType.toRegularClassSymbol(session)
                    ?.declaredMemberScope(session, memberRequiredPhase = null)
                    ?.getDeclaredConstructors()
                    ?.firstOrNull { it.valueParameterSymbols.isEmpty() } ?: return@buildPrimaryConstructor
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
        cachesFactory: FirCachesFactory,
        private val session: FirSession,
        private val classSymbol: FirClassSymbol<*>,
        private val extensionsByClassifierName: ExtensionsByName,
        private val extensionsNeedingCompanionObject: List<FirDeclarationGenerationExtension>,
    ) {
        class Context(
            val extensionsByClassifierName: ExtensionsByName,
            val extensionsNeedingCompanionObject: List<FirDeclarationGenerationExtension>,
        )

        val classifiersCache: FirCache<Name, FirRegularClassSymbol?, NestedClassGenerationContext> =
            cachesFactory.createCache { name, context -> generateNestedClassifier(name, context) }

        val allClassifierNames: Set<Name>
            get() = if (needsCompanionObject) extensionsByClassifierName.keys + DEFAULT_NAME_FOR_COMPANION_OBJECT
            else extensionsByClassifierName.keys

        private val needsCompanionObject get() = extensionsNeedingCompanionObject.isNotEmpty()

        private fun generateNestedClassifier(name: Name, generationContext: NestedClassGenerationContext): FirRegularClassSymbol? {
            if (classSymbol is FirRegularClassSymbol) {
                val companion = classSymbol.companionObjectSymbol
                if (companion != null && companion.origin.generated && companion.classId.shortClassName == name) {
                    return companion
                }
            }

            val alreadyHasCompanion = classSymbol is FirRegularClassSymbol && classSymbol.companionObjectSymbol != null

            val needsCompanion =
                name == DEFAULT_NAME_FOR_COMPANION_OBJECT && !alreadyHasCompanion && needsCompanionObject

            val extensions = extensionsByClassifierName[name].orEmpty()

            val generatedClasses = extensions.mapNotNull { extension ->
                extension.generateNestedClassLikeDeclaration(classSymbol, name, generationContext)?.also { symbol ->
                    symbol.fir.ownerGenerator = extension
                    if (classSymbol.isLocal) {
                        symbol.fir.containingClassForLocalAttr = classSymbol.toLookupTag()
                    }
                }
            }

            if (needsCompanion) {
                if (generatedClasses.isNotEmpty()) error(
                    """
                     Some plugin(s) request a companion object for ${classSymbol.classId} with `needsCompanionObject`,
                     but other plugins generate it with `generateNestedClassLikeDeclaration`:
                    ${generatedClasses.joinToString("\n") { it.fir.render() }}
                """.trimIndent()
                )

                return generateCompanionObjectForPlugins(classSymbol) {
                    for (extension in extensionsNeedingCompanionObject) {
                        val info = extension.getCompanionObjectInfo(classSymbol, generationContext)
                        info.superTypes.mapTo(superTypeRefs) { it.toFirResolvedTypeRef() }
                        annotations.addAll(info.annotations)
                    }
                }.symbol
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

        private fun generateCompanionObjectForPlugins(
            owner: FirClassSymbol<*>,
            configure: FirRegularClassBuilder.() -> Unit,
        ): FirRegularClass = buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = GeneratedCompanionKey.origin
            classKind = ClassKind.OBJECT
            scopeProvider = session.kotlinScopeProvider
            val visibility = if (owner.isLocal) Visibilities.Local else Visibilities.Public
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(owner, forClass = true)
            ).apply {
                isCompanion = true
                isExpect = owner.isExpect
            }
            source = owner.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated.Default)
            name = DEFAULT_NAME_FOR_COMPANION_OBJECT
            symbol = FirRegularClassSymbol(owner.classId.createNestedClassId(DEFAULT_NAME_FOR_COMPANION_OBJECT))
            superTypeRefs += session.builtinTypes.anyType
            configure()
        }.apply {
            if (owner.isLocal) {
                containingClassForLocalAttr = owner.toLookupTag()
            }
        }
    }

    private inline fun groupExtensionsByName(
        classSymbol: FirClassSymbol<*>,
        nameExtractor: FirDeclarationGenerationExtension.(FirClassSymbol<*>) -> Set<Name>,
    ): Map<Name, List<FirDeclarationGenerationExtension>> {
        val extensions = getExtensionsForClass(classSymbol)
        return extensions.flatGroupBy { it.nameExtractor(classSymbol) }
    }

    private fun getExtensionsForClass(classSymbol: FirClassSymbol<*>): List<FirDeclarationGenerationExtension> {
        require(session === classSymbol.moduleData.session) {
            "Class $classSymbol is declared in ${classSymbol.moduleData.session}, but generated storage for it taken from $session"
        }
        // Every plugin can add declarations to the generated companion, not only the plugins that return `true` from `needsCompanionObject`.
        // The companion then behaves like a companion object from source.
        return if (classSymbol.origin.generated && !classSymbol.isLocal && classSymbol.origin != GeneratedCompanionKey.origin) {
            listOf(classSymbol.fir.ownerGenerator!!)
        } else {
            session.extensionService.declarationGenerators
        }
    }
}

private val FirSession.generatedDeclarationsStorage: FirGeneratedMemberDeclarationsStorage by FirSession.sessionComponentAccessor()
