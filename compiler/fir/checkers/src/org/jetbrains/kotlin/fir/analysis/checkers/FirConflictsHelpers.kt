/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl.Companion.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl.Companion.DEFAULT_STATUS_FOR_SUSPEND_MAIN_FUNCTION
import org.jetbrains.kotlin.fir.declarations.impl.modifiersRepresentation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.SmartSet

val DEFAULT_STATUS_FOR_NORMAL_MAIN_FUNCTION = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS

private val FirSimpleFunction.hasMainFunctionStatus
    get() = when (status.modifiersRepresentation) {
        DEFAULT_STATUS_FOR_NORMAL_MAIN_FUNCTION.modifiersRepresentation,
        DEFAULT_STATUS_FOR_SUSPEND_MAIN_FUNCTION.modifiersRepresentation,
        -> true
        else -> false
    }

private val CallableId.isTopLevel get() = className == null

private fun FirDeclaration.isCollectable(): Boolean {
    if (this is FirCallableDeclaration) {
        if (contextReceivers.any { it.typeRef.coneType.hasError() }) return false
        if (typeParameters.any { it.toConeType().hasError() }) return false
        if (receiverParameter?.typeRef?.coneType?.hasError() == true) return false
        if (this is FirFunction && valueParameters.any { it.returnTypeRef.coneType.hasError() }) return false
    }

    return when (this) {
        // - see tests with `fun () {}`.
        // you can't redeclare something that has no name.
        is FirSimpleFunction -> source?.kind !is KtFakeSourceElementKind && name != SpecialNames.NO_NAME_PROVIDED
        is FirRegularClass -> name != SpecialNames.NO_NAME_PROVIDED
        // - see testEnumValuesValueOf.
        // it generates a static function that has
        // the same signature as the function defined
        // explicitly.
        is FirProperty -> source?.kind !is KtFakeSourceElementKind.EnumGeneratedDeclaration
        // class delegation field will be renamed after by the IR backend in a case of a name clash
        is FirField -> source?.kind != KtFakeSourceElementKind.ClassDelegationField
        else -> true
    }
}

private fun isExpectAndActual(declaration1: FirDeclaration, declaration2: FirDeclaration): Boolean {
    if (declaration1 !is FirMemberDeclaration) return false
    if (declaration2 !is FirMemberDeclaration) return false
    return (declaration1.status.isExpect && declaration2.status.isActual) ||
            (declaration1.status.isActual && declaration2.status.isExpect)
}

private class DeclarationBuckets {
    val simpleFunctions = mutableListOf<Pair<FirSimpleFunction, String>>()
    val constructors = mutableListOf<Pair<FirConstructor, String>>()
    val classLikes = mutableListOf<Pair<FirClassLikeDeclaration, String>>()
    val properties = mutableListOf<Pair<FirProperty, String>>()
    val extensionProperties = mutableListOf<Pair<FirProperty, String>>()
}

private fun groupTopLevelByName(declarations: List<FirDeclaration>): Map<Name, DeclarationBuckets> {
    val groups = mutableMapOf<Name, DeclarationBuckets>()
    for (declaration in declarations) {
        if (!declaration.isCollectable()) continue

        when (declaration) {
            is FirSimpleFunction ->
                groups.getOrPut(declaration.name, ::DeclarationBuckets).simpleFunctions +=
                    declaration to FirRedeclarationPresenter.represent(declaration)
            is FirProperty -> {
                val group = groups.getOrPut(declaration.name, ::DeclarationBuckets)
                val representation = FirRedeclarationPresenter.represent(declaration)
                if (declaration.receiverParameter != null) {
                    group.extensionProperties += declaration to representation
                } else {
                    group.properties += declaration to representation
                }
            }
            is FirRegularClass -> {
                val group = groups.getOrPut(declaration.name, ::DeclarationBuckets)
                group.classLikes += declaration to FirRedeclarationPresenter.represent(declaration)
                if (declaration.classKind != ClassKind.OBJECT) {
                    declaration.declarations
                        .filterIsInstance<FirConstructor>()
                        .mapTo(group.constructors) { it to FirRedeclarationPresenter.represent(it, declaration) }
                }
            }
            is FirTypeAlias ->
                groups.getOrPut(declaration.name, ::DeclarationBuckets).classLikes +=
                    declaration to FirRedeclarationPresenter.represent(declaration)
            else -> {}
        }
    }
    return groups
}

/**
 * Collects FirDeclarations for further analysis.
 */
class FirDeclarationCollector<D : FirDeclaration>(
    internal val context: CheckerContext,
) {
    internal val session: FirSession get() = context.sessionHolder.session

    val declarationConflictingSymbols: HashMap<D, SmartSet<FirBasedSymbol<*>>> = hashMapOf()
}

fun FirDeclarationCollector<FirDeclaration>.collectClassMembers(klass: FirRegularClass) {
    val otherDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()
    val functionDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()

    // TODO, KT-61243: Use declaredMemberScope
    for (it in klass.declarations) {
        if (!it.isCollectable()) continue

        when (it) {
            is FirSimpleFunction -> collect(it, FirRedeclarationPresenter.represent(it), functionDeclarations)
            is FirRegularClass -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
            is FirTypeAlias -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
            is FirVariable -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
            else -> {}
        }
    }
}

fun collectConflictingLocalFunctionsFrom(block: FirBlock, context: CheckerContext): Map<FirFunction, Set<FirBasedSymbol<*>>> {
    val collectables =
        block.statements.filter {
            (it is FirSimpleFunction || it is FirRegularClass) && (it as FirDeclaration).isCollectable()
        }

    if (collectables.isEmpty()) return emptyMap()

    val inspector = FirDeclarationCollector<FirFunction>(context)
    val functionDeclarations = mutableMapOf<String, MutableList<FirFunction>>()

    for (collectable in collectables) {
        when (collectable) {
            is FirSimpleFunction ->
                inspector.collect(collectable, FirRedeclarationPresenter.represent(collectable), functionDeclarations)
            is FirRegularClass ->
                // TODO, KT-61243: Use declaredMemberScope
                collectable.declarations.filterIsInstance<FirConstructor>().forEach {
                    inspector.collect(it, FirRedeclarationPresenter.represent(it, collectable), functionDeclarations)
                }
            else -> {}
        }
    }

    return inspector.declarationConflictingSymbols
}

private fun <D : FirDeclaration> FirDeclarationCollector<D>.collect(
    declaration: D,
    representation: String,
    map: MutableMap<String, MutableList<D>>,
) {
    map.getOrPut(representation, ::mutableListOf).also {
        it.add(declaration)

        val conflicts = SmartSet.create<FirBasedSymbol<*>>()
        for (otherDeclaration in it) {
            if (otherDeclaration != declaration && !isOverloadable(declaration, otherDeclaration, session)) {
                conflicts.add(otherDeclaration.symbol)
                declarationConflictingSymbols.getOrPut(otherDeclaration) { SmartSet.create() }.add(declaration.symbol)
            }
        }

        declarationConflictingSymbols[declaration] = conflicts
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
@OptIn(SymbolInternals::class)
@Suppress("GrazieInspection")
fun FirDeclarationCollector<FirDeclaration>.collectTopLevel(file: FirFile, packageMemberScope: FirPackageMemberScope) {

    for ((declarationName, group) in groupTopLevelByName(file.declarations)) {
        val groupHasClassLikesOrProperties = group.classLikes.isNotEmpty() || group.properties.isNotEmpty()
        val groupHasSimpleFunctions = group.simpleFunctions.isNotEmpty()

        fun collect(
            declarations: List<Pair<FirDeclaration, String>>,
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

                session.lookupTracker?.recordLookup(declarationName, file.packageFqName.asString(), declaration.source, file.source)
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
                if (conflictingSymbol !is FirRegularClassSymbol) return
                if (conflictingSymbol.classKind == ClassKind.OBJECT || conflictingSymbol.classKind == ClassKind.ENUM_ENTRY) return

                conflictingSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

                val classWithSameName = conflictingSymbol.fir
                classWithSameName.unsubstitutedScope(context).processDeclaredConstructors { constructor ->
                    val ctorRepresentation = FirRedeclarationPresenter.represent(constructor.fir, classWithSameName)
                    collect(group.simpleFunctions, conflictingSymbol = constructor, conflictingPresentation = ctorRepresentation)
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
            session.nameConflictsTracker?.let { it as? FirNameConflictsTracker }
                ?.redeclaredClassifiers?.get(ClassId(file.packageFqName, declarationName))?.forEach {
                    collectFromClassifierSource(conflictingSymbol = it.classifier, conflictingFile = it.file)
                }

            // session.nameConflictsTracker doesn't seem to work for LL API for redeclarations in the same file, for this reason
            // we explicitly check classLikes in the same file, too.
            for ((classLike, representation) in group.classLikes) {
                collectFromClassifierSource(classLike.symbol, conflictingPresentation = representation, conflictingFile = file)
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

private fun FirDeclarationCollector<FirDeclaration>.collectTopLevelConflict(
    declaration: FirDeclaration,
    declarationPresentation: String,
    containingFile: FirFile,
    conflictingSymbol: FirBasedSymbol<*>,
    conflictingPresentation: String? = null,
    conflictingFile: FirFile? = null,
) {
    conflictingSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    @OptIn(SymbolInternals::class)
    val conflicting = conflictingSymbol.fir
    if (conflicting == declaration || declaration.moduleData != conflicting.moduleData) return
    val actualConflictingPresentation = conflictingPresentation ?: FirRedeclarationPresenter.represent(conflicting)
    if (actualConflictingPresentation != declarationPresentation) return
    val actualConflictingFile =
        conflictingFile ?: when (conflictingSymbol) {
            is FirClassLikeSymbol<*> -> session.firProvider.getFirClassifierContainerFileIfAny(conflictingSymbol)
            is FirCallableSymbol<*> -> session.firProvider.getFirCallableContainerFile(conflictingSymbol)
            else -> null
        }
    if (!conflicting.isCollectable()) return
    if (areCompatibleMainFunctions(declaration, containingFile, conflicting, actualConflictingFile, session)) return
    if (
        conflicting is FirMemberDeclaration &&
        !session.visibilityChecker.isVisible(conflicting, session, containingFile, emptyList(), dispatchReceiver = null)
    ) return
    if (isOverloadable(declaration, conflicting, session)) return

    declarationConflictingSymbols.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
}

private fun FirSimpleFunction.representsMainFunctionAllowingConflictingOverloads(session: FirSession): Boolean {
    if (name != StandardNames.MAIN || !symbol.callableId.isTopLevel || !hasMainFunctionStatus) return false
    if (receiverParameter != null || typeParameters.isNotEmpty()) return false
    if (valueParameters.isEmpty()) return true
    val paramType = valueParameters.singleOrNull()?.returnTypeRef?.coneType?.fullyExpandedType(session) ?: return false
    if (!paramType.isNonPrimitiveArray) return false
    val typeArgument = paramType.typeArguments.singleOrNull() as? ConeKotlinTypeProjection ?: return false
    // only Array<String> and Array<out String> are accepted
    if (typeArgument !is ConeKotlinType && typeArgument !is ConeKotlinTypeProjectionOut) return false
    return typeArgument.type.fullyExpandedType(session).isString
}

private fun areCompatibleMainFunctions(
    declaration1: FirDeclaration, file1: FirFile,
    declaration2: FirDeclaration, file2: FirFile?,
    session: FirSession,
) = file1 != file2
        && declaration1 is FirSimpleFunction
        && declaration2 is FirSimpleFunction
        && declaration1.representsMainFunctionAllowingConflictingOverloads(session)
        && declaration2.representsMainFunctionAllowingConflictingOverloads(session)

private fun isOverloadable(
    declaration: FirDeclaration,
    conflicting: FirDeclaration,
    session: FirSession,
): Boolean {
    if (isExpectAndActual(declaration, conflicting)) return true

    val declarationIsLowPriority = hasLowPriorityAnnotation(declaration.annotations)
    val conflictingIsLowPriority = hasLowPriorityAnnotation(conflicting.annotations)
    if (declarationIsLowPriority != conflictingIsLowPriority) return true

    return declaration is FirCallableDeclaration &&
            conflicting is FirCallableDeclaration &&
            session.declarationOverloadabilityHelper.isOverloadable(declaration, conflicting)
}

/** Checks for redeclarations of value and type parameters, and local variables. */
fun checkForLocalRedeclarations(elements: List<FirElement>, context: CheckerContext, reporter: DiagnosticReporter) {
    if (elements.size <= 1) return

    val multimap = ListMultimap<Name, FirBasedSymbol<*>>()

    for (element in elements) {
        val name: Name?
        val symbol: FirBasedSymbol<*>?
        when (element) {
            is FirVariable -> {
                symbol = element.symbol
                name = element.name
            }
            is FirOuterClassTypeParameterRef -> {
                continue
            }
            is FirTypeParameterRef -> {
                symbol = element.symbol
                name = symbol.name
            }
            else -> {
                symbol = null
                name = null
            }
        }
        if (name?.isSpecial == false) {
            multimap.put(name, symbol!!)
        }
    }
    for (key in multimap.keys) {
        val conflictingElements = multimap[key]
        if (conflictingElements.size > 1) {
            for (conflictingElement in conflictingElements) {
                reporter.reportOn(conflictingElement.source, FirErrors.REDECLARATION, conflictingElements, context)
            }
        }
    }
}
