/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isEffectivelyFinal
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOverloadabilityHelper.ContextParameterShadowing.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl.Companion.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.jetbrains.kotlin.fir.declarations.impl.modifiersRepresentation
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.SmartSet

private fun FirResolvedDeclarationStatus.isAllowedForMainFunction(): Boolean {
    val defaultStatusForMain = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
    if (visibility != defaultStatusForMain.visibility && modality != defaultStatusForMain.modality && effectiveVisibility != defaultStatusForMain.effectiveVisibility) return false
    val cleanedStatus = this.copy(
        // main() function is allowed but not obliged to have the following flags:
        isSuspend = false,
        hasMustUseReturnValue = false
    )
    return cleanedStatus.modifiersRepresentation == defaultStatusForMain.modifiersRepresentation
}

private val CallableId.isTopLevel get() = className == null

private fun FirBasedSymbol<*>.isCollectable(): Boolean {
    if (this is FirCallableSymbol<*>) {
        if (this is FirErrorCallableSymbol<*>) return false
        if (contextParameterSymbols.any { it.resolvedReturnType.hasError() }) return false
        if (typeParameterSymbols.any { it.toConeType().hasError() }) return false
        if (resolvedReceiverType?.hasError() == true) return false
        if (this is FirFunctionSymbol<*> && valueParameterSymbols.any { it.resolvedReturnType.hasError() }) return false
        @OptIn(SymbolInternals::class)
        if (fir.isHiddenToOvercomeSignatureClash == true) return false
    }

    return when (this) {
        // - see tests with `fun () {}`.
        // you can't redeclare something that has no name.
        is FirNamedFunctionSymbol -> isCollectableAccordingToSource && name != SpecialNames.NO_NAME_PROVIDED
        is FirRegularClassSymbol -> name != SpecialNames.NO_NAME_PROVIDED
        // - see testEnumValuesValueOf.
        // it generates a static function that has
        // the same signature as the function defined
        // explicitly.
        is FirPropertySymbol -> source?.kind !is KtFakeSourceElementKind.EnumGeneratedDeclaration
        // class delegation field will be renamed after by the IR backend in a case of a name clash
        is FirFieldSymbol -> source?.kind != KtFakeSourceElementKind.ClassDelegationField
        else -> true
    }
}

private val FirNamedFunctionSymbol.isCollectableAccordingToSource: Boolean
    get() = source?.kind !is KtFakeSourceElementKind || source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers

internal val FirBasedSymbol<*>.resolvedStatus
    get() = when (this) {
        is FirCallableSymbol<*> -> resolvedStatus
        is FirClassLikeSymbol<*> -> resolvedStatus
        else -> null
    }

private fun isAtLeastOneExpect(first: FirBasedSymbol<*>, second: FirBasedSymbol<*>): Boolean =
    first.resolvedStatus?.isExpect == true || second.resolvedStatus?.isExpect == true

private class DeclarationBuckets {
    val simpleFunctions = mutableListOf<Pair<FirNamedFunctionSymbol, String>>()
    val constructors = mutableListOf<Pair<FirConstructorSymbol, String>>()
    val classLikes = mutableListOf<Pair<FirClassLikeSymbol<*>, String>>()
    val properties = mutableListOf<Pair<FirPropertySymbol, String>>()
    val extensionProperties = mutableListOf<Pair<FirPropertySymbol, String>>()
}

context(context: CheckerContext)
private fun groupTopLevelByName(declarations: List<FirDeclaration>): Map<Name, DeclarationBuckets> {
    val groups = mutableMapOf<Name, DeclarationBuckets>()
    for (declaration in declarations) {
        if (!declaration.symbol.isCollectable()) continue

        when (declaration) {
            is FirSimpleFunction ->
                groups.getOrPut(declaration.name, ::DeclarationBuckets).simpleFunctions +=
                    declaration.symbol to FirRedeclarationPresenter.represent(declaration.symbol)
            is FirProperty -> {
                val group = groups.getOrPut(declaration.name, ::DeclarationBuckets)
                val representation = FirRedeclarationPresenter.represent(declaration.symbol)
                if (declaration.receiverParameter != null) {
                    group.extensionProperties += declaration.symbol to representation
                } else {
                    group.properties += declaration.symbol to representation
                }
            }
            is FirClassLikeDeclaration -> {
                val representation = FirRedeclarationPresenter.represent(declaration.symbol) ?: continue
                val group = groups.getOrPut(declaration.nameOrSpecialName, ::DeclarationBuckets)
                group.classLikes += declaration.symbol to representation

                declaration.symbol.expandedClassWithConstructorsScope()?.let { (expandedClass, scopeWithConstructors) ->
                    if (expandedClass.classKind == ClassKind.OBJECT) {
                        return@let
                    }

                    scopeWithConstructors.processDeclaredConstructors {
                        group.constructors += it to FirRedeclarationPresenter.represent(it, declaration.symbol)
                    }
                }
            }
            else -> {}
        }
    }
    return groups
}

/**
 * Collects symbols of FirDeclarations for further analysis.
 */
class FirDeclarationCollector<D : FirBasedSymbol<*>>(
    internal val context: CheckerContext,
) {
    internal val session: FirSession get() = context.sessionHolder.session

    val declarationConflictingSymbols: HashMap<D, SmartSet<FirBasedSymbol<*>>> = hashMapOf()
    val declarationShadowedViaContextParameters: HashMap<D, SmartSet<FirBasedSymbol<*>>> = hashMapOf()
}

fun FirDeclarationCollector<FirBasedSymbol<*>>.collectClassMembers(klass: FirClassSymbol<*>): Unit = with(context) {
    val otherDeclarations = mutableMapOf<String, MutableSet<FirBasedSymbol<*>>>()
    val functionDeclarations = mutableMapOf<String, MutableSet<FirFunctionSymbol<*>>>()
    val declaredMemberScope = klass.declaredMemberScope()
    val unsubstitutedScope = klass.unsubstitutedScope()

    declaredMemberScope.processAllFunctions { declaredFunction ->
        if (!declaredFunction.isCollectable()) {
            return@processAllFunctions
        }

        collect(declaredFunction, FirRedeclarationPresenter.represent(declaredFunction), functionDeclarations)

        unsubstitutedScope.processFunctionsByName(declaredFunction.name) { anotherFunction ->
            if (anotherFunction != declaredFunction && anotherFunction.isCollectable() && anotherFunction.isVisibleInClass(klass)) {
                collect(anotherFunction, FirRedeclarationPresenter.represent(anotherFunction), functionDeclarations)
            }
        }
    }

    // Constructors of nested classes
    // are collected when checking the outer
    // class: this is because they may clash
    // with functions from this outer class,
    // so we should avoid checking them twice.
    if (context.isTopLevel) {
        unsubstitutedScope.processDeclaredConstructors {
            if (it.isCollectable() && it.isVisibleInClass(klass)) {
                collect(it, FirRedeclarationPresenter.represent(it, klass), functionDeclarations)
            }
        }
    }

    declaredMemberScope.processAllProperties { declaredProperty ->
        if (!declaredProperty.isCollectable()) {
            return@processAllProperties
        }

        collect(declaredProperty, FirRedeclarationPresenter.represent(declaredProperty), otherDeclarations)

        unsubstitutedScope.processPropertiesByName(declaredProperty.name) { anotherProperty ->
            if (anotherProperty != declaredProperty && anotherProperty.isCollectable() && anotherProperty.isVisibleInClass(klass)) {
                collect(anotherProperty, FirRedeclarationPresenter.represent(anotherProperty), otherDeclarations)
            }
        }
    }

    fun processClassifier(it: FirClassifierSymbol<*>, isDeclared: Boolean) {
        when {
            !it.isCollectable() || !isDeclared && !it.isVisibleInClass(klass) -> return
            it is FirRegularClassSymbol -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
            it is FirTypeAliasSymbol -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
            else -> {}
        }

        // This `if` can't be merged with the `when`
        // above, because otherwise the smartcast
        // below doesn't work.
        if (it !is FirClassLikeSymbol<*>) {
            return
        }

        it.expandedClassWithConstructorsScope()?.let { (expandedClass, scopeWithConstructors) ->
            // Objects have implicit FirPrimaryConstructors
            if (expandedClass.classKind == ClassKind.OBJECT) {
                return@let
            }

            scopeWithConstructors.processDeclaredConstructors { constructor ->
                collect(constructor, FirRedeclarationPresenter.represent(constructor, it), functionDeclarations)
            }
        }
    }

    // Scopes refer to inner classifiers
    // through maps indexed by names,
    // so only the last declaration is
    // observed when processing all
    // classifiers
    @OptIn(DirectDeclarationsAccess::class)
    for (declaredClassifier in klass.declarationSymbols) {
        if (declaredClassifier is FirClassifierSymbol<*>) {
            processClassifier(declaredClassifier, isDeclared = true)

            unsubstitutedScope.processClassifiersByName(declaredClassifier.name) { anotherClassifier ->
                if (anotherClassifier != declaredClassifier) {
                    processClassifier(anotherClassifier, isDeclared = false)
                }
            }
        }
    }
}

private val FirClassifierSymbol<*>.name: Name
    get() = when (this) {
        is FirClassLikeSymbol -> name
        is FirTypeParameterSymbol -> name
    }

context(context: CheckerContext)
fun collectConflictingLocalFunctionsFrom(
    block: FirBlock
): FirDeclarationCollector<FirFunctionSymbol<*>>? {
    val collectables =
        block.statements.filter {
            (it is FirSimpleFunction || it is FirRegularClass) && (it as FirDeclaration).symbol.isCollectable()
        }

    if (collectables.isEmpty()) return null

    val inspector = FirDeclarationCollector<FirFunctionSymbol<*>>(context)
    val functionDeclarations = mutableMapOf<String, MutableSet<FirFunctionSymbol<*>>>()

    for (collectable in collectables) {
        when (collectable) {
            is FirSimpleFunction ->
                inspector.collect(collectable.symbol, FirRedeclarationPresenter.represent(collectable.symbol), functionDeclarations)
            is FirClassLikeDeclaration -> {
                collectable.symbol.expandedClassWithConstructorsScope()?.let { (_, scopeWithConstructors) ->
                    scopeWithConstructors.processDeclaredConstructors {
                        inspector.collect(it, FirRedeclarationPresenter.represent(it, collectable.symbol), functionDeclarations)
                    }
                }
            }
            else -> {}
        }
    }

    return inspector
}

private fun <D : FirBasedSymbol<*>, S : D> FirDeclarationCollector<D>.collect(
    declaration: S,
    representation: String,
    map: MutableMap<String, MutableSet<S>>,
) {
    map.getOrPut(representation, ::mutableSetOf).also {
        if (!it.add(declaration)) {
            return@also
        }

        val conflicts = SmartSet.create<FirBasedSymbol<*>>()
        val shadowing = SmartSet.create<FirBasedSymbol<*>>()

        for (otherDeclaration in it) {
            if (otherDeclaration != declaration) {
                when (getConflictState(declaration, otherDeclaration)) {
                    ConflictState.Conflict -> {
                        conflicts.add(otherDeclaration)
                        declarationConflictingSymbols.getOrPut(otherDeclaration) { SmartSet.create() }.add(declaration)
                    }
                    ConflictState.ContextParameterShadowing -> {
                        shadowing.add(otherDeclaration)
                    }
                    ConflictState.NoConflict -> {}
                }
            }
        }

        declarationConflictingSymbols[declaration] = conflicts
        declarationShadowedViaContextParameters[declaration] = shadowing
    }
}

/**
 * To check top-level declarations for redeclarations, we check multiple sources (the packageMemberScope's properties, functions
 * and classifiers), redeclared classifiers from session.nameConflictsTracker and the file's declarations themselves.
 * To prevent inspecting the same source multiple times, we group the declarations in the file by name and subdivide them into
 * buckets (the properties of DeclarationGroup).
 *
 * Depending on the presence of declarations in the buckets, some checks can be omitted.
 * E.g., if there are no functions and no classes with constructors in the file, we don't need to inspect functions.
 *
 * #### Matrix of possible conflicts between "sources" and "buckets"
 *
 * |                         | simpleFunctions | constructors | classLikes | Properties | extensionProperties |
 * |-------------------------|-----------------|--------------|------------|------------|---------------------|
 * | functions               | X               | X            |            |            |                     |
 * | classifiers             |                 |              | X          | X          |                     |
 * | constructors of classes | X               |              |            |            |                     |
 * | properties              |                 |              | X          | X          | X                   |
 */
@Suppress("GrazieInspection")
fun FirDeclarationCollector<FirBasedSymbol<*>>.collectTopLevel(
    file: FirFile, packageMemberScope: FirPackageMemberScope
): Unit = with(context) {
    @OptIn(DirectDeclarationsAccess::class)
    for ((declarationName, group) in groupTopLevelByName(file.declarations)) {
        val groupHasClassLikesOrProperties = group.classLikes.isNotEmpty() || group.properties.isNotEmpty()
        val groupHasSimpleFunctions = group.simpleFunctions.isNotEmpty()

        fun collect(
            declarations: List<Pair<FirBasedSymbol<*>, String>>,
            conflictingSymbol: FirBasedSymbol<*>,
            conflictingPresentation: String? = null,
            conflictingFile: FirFile? = null,
        ) {
            for ((declaration, declarationPresentation) in declarations) {
                collectTopLevelConflict(
                    declaration,
                    declarationPresentation,
                    file,
                    conflictingSymbol,
                    conflictingPresentation,
                    conflictingFile
                )

                session.lookupTracker?.recordNameLookup(declarationName, file.packageFqName.asString(), declaration.source, file.source)
            }
        }

        fun collectFromClassifierSource(
            conflictingSymbol: FirClassifierSymbol<*>,
            conflictingPresentation: String? = null,
            conflictingFile: FirFile? = null,
        ) {
            collect(group.classLikes, conflictingSymbol, conflictingPresentation, conflictingFile)
            collect(group.properties, conflictingSymbol, conflictingPresentation, conflictingFile)

            if (groupHasSimpleFunctions) {
                if (conflictingSymbol !is FirClassLikeSymbol<*>) {
                    return
                }

                conflictingSymbol.expandedClassWithConstructorsScope()?.let { (expandedClass, scopeWithConstructors) ->
                    if (expandedClass.classKind == ClassKind.OBJECT || expandedClass.classKind == ClassKind.ENUM_ENTRY) {
                        return
                    }

                    scopeWithConstructors.processDeclaredConstructors { constructor ->
                        val ctorRepresentation = FirRedeclarationPresenter.represent(constructor, conflictingSymbol)
                        collect(group.simpleFunctions, conflictingSymbol = constructor, conflictingPresentation = ctorRepresentation)
                    }
                }
            }
        }

        // Check sources in the order from the table above. Skip the check if all relevant buckets are empty.

        // Function source
        if (groupHasSimpleFunctions || group.constructors.isNotEmpty()) {
            packageMemberScope.processFunctionsByName(declarationName) {
                collect(group.simpleFunctions, it)
                collect(group.constructors, it)
            }
        }

        // Classifier sources, collectForClassifierSource will also check constructors.
        if (groupHasClassLikesOrProperties || groupHasSimpleFunctions) {
            // Scope will only return one classifier per name
            packageMemberScope.processClassifiersByNameWithSubstitution(declarationName) { symbol, _ ->
                collectFromClassifierSource(conflictingSymbol = symbol)
            }

            // session.nameConflictsTracker will contain more classifiers with the same name.
            session.nameConflictsTracker
                ?.getClassifierRedeclarations(ClassId(file.packageFqName, declarationName))
                ?.forEach { redeclaration ->
                    collectFromClassifierSource(
                        conflictingSymbol = redeclaration.classifierSymbol,
                        conflictingFile = redeclaration.containingFile,
                    )
                }

            // session.nameConflictsTracker doesn't seem to work for LL API for redeclarations in the same file, for this reason
            // we explicitly check classLikes in the same file, too.
            for ((classLike, representation) in group.classLikes) {
                collectFromClassifierSource(classLike, conflictingPresentation = representation, conflictingFile = file)
            }
        }

        // Property source
        if (groupHasClassLikesOrProperties || group.extensionProperties.isNotEmpty()) {
            packageMemberScope.processPropertiesByName(declarationName) {
                collect(group.classLikes, conflictingSymbol = it)
                collect(group.properties, conflictingSymbol = it)
                collect(group.extensionProperties, conflictingSymbol = it)
            }
        }
    }
}

context(context: CheckerContext)
private fun FirClassLikeSymbol<*>.expandedClassWithConstructorsScope(): Pair<FirRegularClassSymbol, FirScope>? =
    expandedClassWithConstructorsScope(context.session, context.scopeSession, FirResolvePhase.STATUS)

private fun shouldCheckForMultiplatformRedeclaration(dependency: FirBasedSymbol<*>, dependent: FirBasedSymbol<*>): Boolean {
    if (dependency.moduleData !in dependent.moduleData.allDependsOnDependencies) return false

    /*
     * If one of declarations is expect and the other is not expect, ExpectActualChecker will handle this case
     * All other cases (both are expect or both are not expect) should be reported as declarations conflict
     */
    return !isAtLeastOneExpect(dependency, dependent)
}

private fun FirDeclarationCollector<FirBasedSymbol<*>>.collectTopLevelConflict(
    declaration: FirBasedSymbol<*>,
    declarationPresentation: String,
    containingFile: FirFile,
    conflictingSymbol: FirBasedSymbol<*>,
    conflictingPresentation: String? = null,
    conflictingFile: FirFile? = null,
) {
    conflictingSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    if (conflictingSymbol == declaration) return
    if (
        declaration.moduleData != conflictingSymbol.moduleData &&
        !shouldCheckForMultiplatformRedeclaration(declaration, conflictingSymbol)
    ) return
    val actualConflictingPresentation = conflictingPresentation ?: FirRedeclarationPresenter.represent(conflictingSymbol)
    if (actualConflictingPresentation != declarationPresentation) return
    val actualConflictingFile =
        conflictingFile ?: when (conflictingSymbol) {
            is FirClassLikeSymbol<*> -> session.firProvider.getFirClassifierContainerFileIfAny(conflictingSymbol)
            is FirCallableSymbol<*> -> session.firProvider.getFirCallableContainerFile(conflictingSymbol)
            else -> null
        }
    if (!conflictingSymbol.isCollectable()) return
    if (areCompatibleMainFunctions(declaration, containingFile, conflictingSymbol, actualConflictingFile, session)) return

    @OptIn(SymbolInternals::class)
    val conflicting = conflictingSymbol.fir

    // Private callables are allowed to be redeclared across multiple files, but private classifiers are not. Hence, we only check the
    // visibility of callables, as classifiers of any visibility can conflict.
    if (
        conflicting is FirCallableDeclaration &&
        !session.visibilityChecker.isVisible(conflicting, session, containingFile, emptyList(), dispatchReceiver = null)
    ) return

    when (getConflictState(declaration, conflictingSymbol)) {
        ConflictState.Conflict -> declarationConflictingSymbols
        ConflictState.ContextParameterShadowing -> declarationShadowedViaContextParameters
        ConflictState.NoConflict -> return
    }.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
}

private fun FirNamedFunctionSymbol.representsMainFunctionAllowingConflictingOverloads(session: FirSession): Boolean {
    if (name != StandardNames.MAIN || !callableId.isTopLevel || !resolvedStatus.isAllowedForMainFunction()) return false
    if (receiverParameterSymbol != null || typeParameterSymbols.isNotEmpty() || hasContextParameters) return false
    val returnType = resolvedReturnType.fullyExpandedType(session)
    if (!returnType.isUnit) return false
    if (valueParameterSymbols.isEmpty()) return true
    val paramType = valueParameterSymbols.singleOrNull()?.resolvedReturnTypeRef?.coneType?.fullyExpandedType(session) ?: return false
    if (!paramType.isNonPrimitiveArray) return false
    val typeArgument = paramType.typeArguments.singleOrNull() as? ConeKotlinTypeProjection ?: return false
    // only Array<String> and Array<out String> are accepted
    if (typeArgument !is ConeKotlinType && typeArgument !is ConeKotlinTypeProjectionOut) return false
    return typeArgument.type.fullyExpandedType(session).isString
}

private fun areCompatibleMainFunctions(
    declaration1: FirBasedSymbol<*>, file1: FirFile,
    declaration2: FirBasedSymbol<*>, file2: FirFile?,
    session: FirSession,
) = file1 != file2
        && declaration1 is FirNamedFunctionSymbol
        && declaration2 is FirNamedFunctionSymbol
        && declaration1.representsMainFunctionAllowingConflictingOverloads(session)
        && declaration2.representsMainFunctionAllowingConflictingOverloads(session)

private enum class ConflictState {
    Conflict,
    ContextParameterShadowing,
    NoConflict,
}

private fun FirDeclarationCollector<*>.getConflictState(
    declaration: FirBasedSymbol<*>,
    conflicting: FirBasedSymbol<*>,
): ConflictState {
    if (isAtLeastOneExpect(declaration, conflicting) && declaration.moduleData != conflicting.moduleData) return ConflictState.NoConflict

    val declarationIsLowPriority = hasLowPriorityAnnotation(declaration.resolvedAnnotationsWithClassIds)
    val conflictingIsLowPriority = hasLowPriorityAnnotation(conflicting.resolvedAnnotationsWithClassIds)
    if (declarationIsLowPriority != conflictingIsLowPriority) return ConflictState.NoConflict

    if (declaration !is FirCallableSymbol<*> || conflicting !is FirCallableSymbol<*>) return ConflictState.Conflict

    val declarationIsFinal = declaration.isEffectivelyFinal()
    val conflictingIsFinal = conflicting.isEffectivelyFinal()

    if (declarationIsFinal && conflictingIsFinal) {
        val declarationIsHidden = declaration.isDeprecationLevelHidden(session)
        if (declarationIsHidden) return ConflictState.NoConflict

        val conflictingIsHidden = conflicting.isDeprecationLevelHidden(session)
        if (conflictingIsHidden) return ConflictState.NoConflict
    }

    val overloadabilityHelper = session.declarationOverloadabilityHelper

    return if (session.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
        overloadabilityHelper.getConflictStateWithContextParameters(declaration, conflicting)
    } else if (overloadabilityHelper.isConflicting(declaration, conflicting)) {
        ConflictState.Conflict
    } else {
        ConflictState.NoConflict
    }
}

private fun FirDeclarationOverloadabilityHelper.getConflictStateWithContextParameters(
    declaration: FirCallableSymbol<*>,
    conflicting: FirCallableSymbol<*>,
): ConflictState {
    return when (getContextParameterShadowing(declaration, conflicting)) {
        BothWays -> ConflictState.Conflict
        Shadowing -> ConflictState.ContextParameterShadowing
        None -> ConflictState.NoConflict
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
/** Checks for redeclarations of value and type parameters, and local variables. */
fun checkForLocalRedeclarations(elements: List<FirElement>) {
    if (elements.size <= 1) return

    val multimap = ListMultimap<Name, FirBasedSymbol<*>>()

    for (element in elements) {
        val (symbol, name) = when (element) {
            is FirProperty -> {
                // Enable snippet specific handling of the local delegated extension properties
                if (element.isReplSnippetDeclaration == true && element.delegate != null && element.receiverParameter != null) {
                    // TODO: relying on an unreliable debug representation of the cone type; see KT-77396
                    element.symbol to Name.identifier("${element.receiverParameter?.typeRef?.coneType}.${element.name}")
                } else {
                    element.symbol to element.name
                }
            }
            is FirVariable -> element.symbol to element.name
            is FirClassLikeDeclaration -> element.symbol.let { it to it.name }
            is FirOuterClassTypeParameterRef -> continue
            is FirTypeParameterRef -> element.symbol.let { it to it.name }
            else -> null to null
        }
        if (name?.isSpecial == false) {
            multimap.put(name, symbol!!)
        }
    }
    for (key in multimap.keys) {
        val conflictingElements = multimap[key]
        if (conflictingElements.size > 1) {
            for (conflictingElement in conflictingElements) {
                reporter.reportOn(conflictingElement.source, FirErrors.REDECLARATION, conflictingElements)
            }
        }
    }
}
