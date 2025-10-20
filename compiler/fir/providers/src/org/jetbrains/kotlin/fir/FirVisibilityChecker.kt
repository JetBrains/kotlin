/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.packageName
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

abstract class FirModuleVisibilityChecker : FirSessionComponent {
    abstract fun isInFriendModule(declaration: FirMemberDeclaration): Boolean

    class Standard(val session: FirSession) : FirModuleVisibilityChecker() {
        override fun isInFriendModule(declaration: FirMemberDeclaration): Boolean {
            return session.moduleData.canSeeInternalsOf(declaration.moduleData)
        }
    }
}

/**
 * Extension to support cases where private declarations are visible despite the different module.
 */
abstract class FirPrivateVisibleFromDifferentModuleExtension : FirSessionComponent {
    abstract fun canSeePrivateDeclarationsOfModule(otherModuleData: FirModuleData): Boolean
    abstract fun canSeePrivateTopLevelDeclarationsFromFile(useSiteFile: FirFile, targetFile: FirFile): Boolean
}

abstract class FirVisibilityChecker : FirComposableSessionComponent<FirVisibilityChecker> {
    @NoMutableState
    object Default : FirVisibilityChecker() {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            symbol: FirBasedSymbol<*>,
            useSiteFile: FirFile,
            containingDeclarations: List<FirDeclaration>,
            dispatchReceiver: FirExpression?,
            session: FirSession,
            isCallToPropertySetter: Boolean,
            supertypeSupplier: SupertypeSupplier
        ): Boolean {
            return true
        }

        override fun platformOverrideVisibilityCheck(
            packageNameOfDerivedClass: FqName,
            symbolInBaseClass: FirBasedSymbol<*>,
            visibilityInBaseClass: Visibility,
        ): Boolean {
            return true
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirVisibilityChecker>): Composed {
        return Composed(components)
    }

    class Composed(
        override val components: List<FirVisibilityChecker>
    ) : FirVisibilityChecker(), FirComposableSessionComponent.Composed<FirVisibilityChecker> {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            symbol: FirBasedSymbol<*>,
            useSiteFile: FirFile,
            containingDeclarations: List<FirDeclaration>,
            dispatchReceiver: FirExpression?,
            session: FirSession,
            isCallToPropertySetter: Boolean,
            supertypeSupplier: SupertypeSupplier,
        ): Boolean {
            return components.all {
                it.platformVisibilityCheck(
                    declarationVisibility,
                    symbol,
                    useSiteFile,
                    containingDeclarations,
                    dispatchReceiver,
                    session,
                    isCallToPropertySetter,
                    supertypeSupplier
                )
            }
        }

        override fun platformOverrideVisibilityCheck(
            packageNameOfDerivedClass: FqName,
            symbolInBaseClass: FirBasedSymbol<*>,
            visibilityInBaseClass: Visibility,
        ): Boolean {
            return components.all {
                it.platformOverrideVisibilityCheck(
                    packageNameOfDerivedClass,
                    symbolInBaseClass,
                    visibilityInBaseClass
                )
            }
        }
    }

    fun isClassLikeVisible(
        declaration: FirClassLikeDeclaration,
        session: FirSession,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
    ): Boolean {
        return isVisible(
            declaration,
            session,
            useSiteFile,
            containingDeclarations,
            dispatchReceiver = null,
            isCallToPropertySetter = false,
            staticQualifierClassForCallable = null,
            skipCheckForContainingClassVisibility = false,
            supertypeSupplier = SupertypeSupplier.Default
        )
    }

    fun isVisible(
        declaration: FirMemberDeclaration,
        session: FirSession,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: FirExpression?,
        isCallToPropertySetter: Boolean = false,
        staticQualifierClassForCallable: FirRegularClass? = null,
        // There's no need to check if containing class is visible in case we check if a member might be overridden in a subclass
        // because visibility for its supertype that contain overridden member is being checked when resolving the type reference.
        // Such flag is not necessary in FE1.0, since there are full structure of fake overrides and containing declaration for overridden
        // is always visible since it's a supertype of a derived class.
        skipCheckForContainingClassVisibility: Boolean = false,
        supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default
    ): Boolean {
        if (!isSpecificDeclarationVisible(
                if (declaration is FirCallableDeclaration) declaration.originalOrSelf() else declaration,
                session,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver,
                isCallToPropertySetter,
                supertypeSupplier
            )
        ) {
            return false
        }

        if (skipCheckForContainingClassVisibility) return true

        if (staticQualifierClassForCallable != null) {
            return isSpecificDeclarationVisible(
                staticQualifierClassForCallable,
                session,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver = null,
                isCallToPropertySetter,
                supertypeSupplier
            )
        }
        return declaration.parentDeclarationSequence(session, dispatchReceiver, containingDeclarations, supertypeSupplier)?.all { parent ->
            isSpecificDeclarationVisible(
                parent,
                session,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver = null,
                isCallToPropertySetter,
                supertypeSupplier
            )
        } ?: true
    }

    fun isVisibleForOverriding(
        candidateInDerivedClass: FirCallableDeclaration,
        candidateInBaseClass: FirCallableDeclaration,
    ): Boolean = isVisibleForOverriding(
        candidateInDerivedClass.moduleData, candidateInDerivedClass.symbol.callableId.packageName, candidateInBaseClass
    )

    fun isVisibleForOverriding(
        derivedClassModuleData: FirModuleData,
        symbolFromDerivedClass: FirClassSymbol<*>,
        candidateInBaseClass: FirCallableDeclaration,
    ): Boolean = isVisibleForOverriding(derivedClassModuleData, symbolFromDerivedClass.classId.packageFqName, candidateInBaseClass)

    private fun isVisibleForOverriding(
        derivedClassModuleData: FirModuleData,
        packageNameOfDerivedClass: FqName,
        candidateInBaseClass: FirCallableDeclaration,
    ): Boolean = isSpecificDeclarationVisibleForOverriding(
        derivedClassModuleData,
        packageNameOfDerivedClass,
        // It is important for package-private visibility as fake override can be in another package
        candidateInBaseClass.originalOrSelf(),
    )

    private fun isSpecificDeclarationVisibleForOverriding(
        derivedClassModuleData: FirModuleData,
        packageNameOfDerivedClass: FqName,
        candidateInBaseClass: FirCallableDeclaration,
    ): Boolean = when (candidateInBaseClass.visibility) {
        Visibilities.Internal -> {
            candidateInBaseClass.moduleData == derivedClassModuleData ||
                    derivedClassModuleData.session.moduleVisibilityChecker?.isInFriendModule(candidateInBaseClass) == true
        }

        Visibilities.Private, Visibilities.PrivateToThis -> false
        Visibilities.Protected -> true
        else -> {

            platformOverrideVisibilityCheck(
                packageNameOfDerivedClass,
                candidateInBaseClass.symbol,
                candidateInBaseClass.visibility
            )
        }
    }

    private fun isSpecificDeclarationVisible(
        declaration: FirMemberDeclaration,
        session: FirSession,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: FirExpression?,
        isCallToPropertySetter: Boolean = false,
        supertypeSupplier: SupertypeSupplier
    ): Boolean {
        val symbol = declaration.symbol

        return when (declaration.visibility) {
            Visibilities.Internal -> {
                declaration.moduleData == session.moduleData || session.moduleVisibilityChecker?.isInFriendModule(declaration) == true
            }
            Visibilities.Private, Visibilities.PrivateToThis -> {
                val ownerLookupTag = symbol.getOwnerLookupTag()
                if (canSeePrivateDeclarationsOfModule(session, declaration.moduleData)) {
                    when {
                        ownerLookupTag == null -> {
                            // Top-level: visible in file
                            canSeePrivateTopLevelDeclarationFromFile(session, useSiteFile, symbol)
                        }
                        else -> {
                            // Member: visible inside parent class, including all its member classes
                            canSeePrivateMemberOf(
                                symbol,
                                containingDeclarations,
                                ownerLookupTag,
                                dispatchReceiver,
                                isVariableOrNamedFunction = symbol.isVariableOrNamedFunction(),
                                session
                            )
                        }
                    }
                } else {
                    declaration is FirNamedFunction && declaration.isAllowedToBeAccessedFromOutside()
                }
            }

            Visibilities.Protected -> {
                val ownerId = symbol.getOwnerLookupTag()
                ownerId != null && canSeeProtectedMemberOf(
                    symbol, containingDeclarations, dispatchReceiver, ownerId, session,
                    isVariableOrNamedFunction = symbol.isVariableOrNamedFunction(),
                    symbol.fir is FirSyntheticPropertyAccessor,
                    supertypeSupplier
                )
            }

            else -> platformVisibilityCheck(
                declaration.visibility,
                symbol,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver,
                session,
                isCallToPropertySetter,
                supertypeSupplier
            )
        }
    }

    protected abstract fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        symbol: FirBasedSymbol<*>,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: FirExpression?,
        session: FirSession,
        isCallToPropertySetter: Boolean,
        supertypeSupplier: SupertypeSupplier
    ): Boolean

    protected abstract fun platformOverrideVisibilityCheck(
        packageNameOfDerivedClass: FqName,
        symbolInBaseClass: FirBasedSymbol<*>,
        visibilityInBaseClass: Visibility,
    ): Boolean

    private fun canSeePrivateMemberOf(
        symbol: FirBasedSymbol<*>,
        containingDeclarationOfUseSite: List<FirDeclaration>,
        ownerLookupTag: ConeClassLikeLookupTag,
        dispatchReceiver: FirExpression?,
        isVariableOrNamedFunction: Boolean,
        session: FirSession
    ): Boolean {
        ownerLookupTag.ownerIfCompanion(session)?.let { companionOwnerLookupTag ->
            return canSeePrivateMemberOf(
                symbol,
                containingDeclarationOfUseSite,
                companionOwnerLookupTag,
                dispatchReceiver,
                isVariableOrNamedFunction,
                session
            )
        }

        // Note: private static symbols aren't accessible by use-site dispatch receiver
        // See e.g. diagnostics/tests/scopes/inheritance/statics/hidePrivateByPublic.kt,
        // private A.a becomes visible from outside without filtering static callables here
        if (dispatchReceiver != null && (symbol !is FirCallableSymbol || !symbol.isStatic)) {
            val fir = symbol.fir
            val dispatchReceiverParameterClassSymbol =
                (fir as? FirCallableDeclaration)
                    ?.propertyIfAccessor?.propertyIfBackingField
                    ?.dispatchReceiverClassLookupTagOrNull()?.toSymbol(session)
                    ?: return true

            val dispatchReceiverParameterClassLookupTag = dispatchReceiverParameterClassSymbol.toLookupTag()
            val dispatchReceiverValueOwnerLookupTag =
                dispatchReceiver.resolvedType.findClassRepresentation(
                    dispatchReceiverParameterClassLookupTag.constructClassType(
                        Array(dispatchReceiverParameterClassSymbol.fir.typeParameters.size) { ConeStarProjection },
                        isMarkedNullable = true
                    ),
                    session,
                )

            if (dispatchReceiverParameterClassLookupTag != dispatchReceiverValueOwnerLookupTag) return false
            if (fir.visibility == Visibilities.PrivateToThis) {
                when (dispatchReceiver) {
                    is FirThisReceiverExpression -> {
                        if (dispatchReceiver.calleeReference.boundSymbol != dispatchReceiverParameterClassSymbol) {
                            return false
                        }
                    }
                    else -> return false
                }
            }
        }

        for (declaration in containingDeclarationOfUseSite) {
            if (declaration !is FirClass) continue
            val boundSymbol = declaration.symbol
            if (boundSymbol.toLookupTag() == ownerLookupTag) {
                return true
            }
        }

        return false
    }

    private fun ConeClassLikeLookupTag.ownerIfCompanion(session: FirSession): ConeClassLikeLookupTag? {
        val outerClassId = classId.outerClassId ?: return null
        val ownerSymbol = toSymbol(session)
        if (ownerSymbol?.isLocal == true) return null

        if ((ownerSymbol as? FirRegularClassSymbol)?.fir?.isCompanion == true) {
            return outerClassId.toLookupTag()
        }
        return null
    }

    private fun canSeeProtectedMemberOf(
        containingUseSiteClass: FirClass,
        dispatchReceiver: FirExpression?,
        ownerLookupTag: ConeClassLikeLookupTag,
        session: FirSession,
        isVariableOrNamedFunction: Boolean,
        isSyntheticProperty: Boolean,
        supertypeSupplier: SupertypeSupplier
    ): Boolean {
        dispatchReceiver?.ownerIfCompanion(session)?.let { companionOwnerLookupTag ->
            if (containingUseSiteClass.isSubclassOf(companionOwnerLookupTag, session, isStrict = false, supertypeSupplier)) return true
        }

        return when {
            !containingUseSiteClass.isSubclassOf(ownerLookupTag, session, isStrict = false, supertypeSupplier) -> false
            isVariableOrNamedFunction -> doesReceiverFitForProtectedVisibility(
                dispatchReceiver,
                containingUseSiteClass,
                ownerLookupTag,
                isSyntheticProperty,
                session
            )
            else -> true
        }
    }

    private fun doesReceiverFitForProtectedVisibility(
        dispatchReceiver: FirExpression?,
        containingUseSiteClass: FirClass,
        ownerLookupTag: ConeClassLikeLookupTag,
        isSyntheticProperty: Boolean,
        session: FirSession
    ): Boolean {
        if (dispatchReceiver == null) return true
        var dispatchReceiverType = dispatchReceiver.resolvedType
        if (dispatchReceiver is FirSuperReceiverExpression) {
            // Special 'super' case: type of this, not of super, should be taken for the check below
            dispatchReceiverType = dispatchReceiver.dispatchReceiver!!.resolvedType
        }
        val typeCheckerState = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        if (AbstractTypeChecker.isSubtypeOf(
                typeCheckerState,
                dispatchReceiverType.fullyExpandedType(session),
                containingUseSiteClass.symbol.constructStarProjectedType()
            )
        ) {
            return true
        }

        if (isSyntheticProperty) {
            return containingUseSiteClass.classId.packageFqName == ownerLookupTag.classId.packageFqName
        }

        return false
    }

    private fun FirExpression?.ownerIfCompanion(session: FirSession): ConeClassLikeLookupTag? =
        // TODO: what if there is an intersection type from smartcast?
        (this?.resolvedType?.lowerBoundIfFlexible() as? ConeClassLikeType)?.lookupTag?.ownerIfCompanion(session)

    // monitorEnter/monitorExit are the only functions which are accessed "illegally" (see kotlin/util/Synchronized.kt).
    // Since they are intrinsified in the codegen, FIR should treat it as visible.
    private fun FirNamedFunction.isAllowedToBeAccessedFromOutside(): Boolean {
        if (!isFromLibrary) return false
        val packageName = symbol.callableId.packageName.asString()
        val name = name.asString()
        return packageName == "kotlin.jvm.internal.unsafe" &&
                (name == "monitorEnter" || name == "monitorExit")
    }

    protected fun canSeeProtectedMemberOf(
        usedSymbol: FirBasedSymbol<*>,
        containingDeclarationOfUseSite: List<FirDeclaration>,
        dispatchReceiver: FirExpression?,
        ownerLookupTag: ConeClassLikeLookupTag,
        session: FirSession,
        isVariableOrNamedFunction: Boolean,
        isSyntheticProperty: Boolean,
        supertypeSupplier: SupertypeSupplier
    ): Boolean {
        if (canSeePrivateMemberOf(
                usedSymbol,
                containingDeclarationOfUseSite,
                ownerLookupTag,
                dispatchReceiver,
                isVariableOrNamedFunction,
                session
            )
        ) return true

        for (containingDeclaration in containingDeclarationOfUseSite) {
            if (containingDeclaration is FirClass) {
                val boundSymbol = containingDeclaration.symbol
                if (canSeeProtectedMemberOf(
                        boundSymbol.fir,
                        dispatchReceiver,
                        ownerLookupTag,
                        session,
                        isVariableOrNamedFunction,
                        isSyntheticProperty,
                        supertypeSupplier
                    )
                ) return true
            } else if (containingDeclaration is FirFile) {
                if (isSyntheticProperty && containingDeclaration.packageFqName == ownerLookupTag.classId.packageFqName) {
                    return true
                }
            }
        }

        return false
    }

    private fun canSeePrivateDeclarationsOfModule(session: FirSession, otherModuleData: FirModuleData): Boolean {
        return session.moduleData == otherModuleData ||
                session.privateVisibleFromDifferentModulesExtension?.canSeePrivateDeclarationsOfModule(otherModuleData) == true
    }

    private fun canSeePrivateTopLevelDeclarationFromFile(
        session: FirSession,
        useSiteFile: FirFile,
        declarationSymbol: FirBasedSymbol<*>,
    ): Boolean {
        val declarationContainingFile = declarationSymbol.moduleData.session.firProvider.getContainingFile(declarationSymbol)
            ?: return false
        return useSiteFile == declarationContainingFile ||
                session.privateVisibleFromDifferentModulesExtension?.canSeePrivateTopLevelDeclarationsFromFile(
                    useSiteFile,
                    declarationContainingFile
                ) == true
    }
}

val FirSession.moduleVisibilityChecker: FirModuleVisibilityChecker? by FirSession.nullableSessionComponentAccessor()
private val FirSession.privateVisibleFromDifferentModulesExtension: FirPrivateVisibleFromDifferentModuleExtension? by FirSession.nullableSessionComponentAccessor()
val FirSession.visibilityChecker: FirVisibilityChecker by FirSession.sessionComponentAccessorWithDefault(FirVisibilityChecker.Default)

fun FirBasedSymbol<*>.getOwnerLookupTag(): ConeClassLikeLookupTag? {
    return when (this) {
        is FirBackingFieldSymbol -> fir.propertySymbol.getOwnerLookupTag()
        is FirClassLikeSymbol<*> -> getContainingClassLookupTag()
        is FirCallableSymbol<*> -> containingClassLookupTag()
        is FirScriptSymbol, is FirCodeFragmentSymbol -> null
        else -> errorWithAttachment("Unsupported owner search for ${fir::class.java}") {
            withFirEntry("ownerDeclaration", fir)
        }
    }
}

fun FirBasedSymbol<*>.isVariableOrNamedFunction(): Boolean {
    return this is FirVariableSymbol || this is FirNamedFunctionSymbol || this is FirPropertyAccessorSymbol
}


fun FirMemberDeclaration.parentDeclarationSequence(
    session: FirSession,
    dispatchReceiver: FirExpression?,
    containingDeclarations: List<FirDeclaration>,
    supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
): Sequence<FirClassLikeDeclaration>? {
    val parentClass = containingNonLocalClass(
        session,
        dispatchReceiver,
        containingDeclarations,
        supertypeSupplier
    ) ?: return null

    return generateSequence(parentClass) { it.containingNonLocalClass(session) }
}

private fun FirMemberDeclaration.containingNonLocalClass(
    session: FirSession,
    dispatchReceiver: FirExpression?,
    containingUseSiteDeclarations: List<FirDeclaration>,
    supertypeSupplier: SupertypeSupplier
): FirClassLikeDeclaration? {
    return when (this) {
        is FirCallableDeclaration -> {
            if (dispatchReceiver != null) {
                val baseReceiverType = dispatchReceiverClassTypeOrNull()
                if (baseReceiverType != null) {
                    dispatchReceiver.resolvedType.findClassRepresentation(baseReceiverType, session)?.toSymbol(session)?.fir?.let {
                        return it
                    }
                }
            }

            val containingLookupTag = this.containingClassLookupTag()
            val containingClass = containingLookupTag?.toSymbol(session)?.fir

            if (isStatic && containingClass != null) {
                containingUseSiteDeclarations.firstNotNullOfOrNull {
                    if (it !is FirClass) return@firstNotNullOfOrNull null
                    it.takeIf { it.isSubclassOf(containingLookupTag, session, isStrict = false, supertypeSupplier) }
                }?.let { return it }
            }

            containingClass
        }
        is FirClassLikeDeclaration -> containingNonLocalClass(session)
    }
}

private fun FirClassLikeDeclaration.containingNonLocalClass(session: FirSession): FirClassLikeDeclaration? {
    return when (this) {
        is FirClass -> {
            if (isLocal) return null

            this.classId.outerClassId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it)?.fir }
        }
        // Currently, type aliases are only top-level
        is FirTypeAlias -> null
    }
}

fun FirVisibilityChecker.isVisible(
    symbol: FirBasedSymbol<*>,
    session: FirSession,
    useSiteFileSymbol: FirFileSymbol,
    containingDeclarations: List<FirBasedSymbol<*>>,
    dispatchReceiver: FirExpression?,
    skipCheckForContainingClassVisibility: Boolean = false,
): Boolean {
    symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    val declaration = symbol.fir as? FirMemberDeclaration ?: error("Not a member declaration: $symbol")
    return isVisible(
        declaration = declaration,
        session,
        useSiteFile = useSiteFileSymbol.fir,
        containingDeclarations.map { it.fir },
        dispatchReceiver,
        skipCheckForContainingClassVisibility = skipCheckForContainingClassVisibility,
    )
}

fun FirVisibilityChecker.isClassLikeVisible(
    symbol: FirClassLikeSymbol<*>,
    session: FirSession,
    useSiteFileSymbol: FirFileSymbol,
    containingDeclarations: List<FirBasedSymbol<*>>,
): Boolean {
    symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
    return isClassLikeVisible(
        declaration = symbol.fir,
        session,
        useSiteFileSymbol.fir,
        containingDeclarations.map { it.fir },
    )
}

fun FirCallableDeclaration.isVisibleInClass(parentClass: FirClass): Boolean {
    return symbol.isVisibleInClass(parentClass.symbol, symbol.resolvedStatus)
}

fun FirBasedSymbol<*>.isVisibleInClass(parentClassSymbol: FirClassSymbol<*>): Boolean {
    val status = when (this) {
        is FirCallableSymbol<*> -> resolvedStatus
        is FirClassLikeSymbol -> resolvedStatus
        else -> return true
    }
    return isVisibleInClass(parentClassSymbol, status)
}

fun FirBasedSymbol<*>.isVisibleInClass(classSymbol: FirClassSymbol<*>, status: FirDeclarationStatus): Boolean {
    val classPackage = classSymbol.classId.packageFqName
    val packageName = when (this) {
        is FirCallableSymbol<*> -> callableId.packageName
        is FirClassLikeSymbol<*> -> classId.packageFqName
        else -> return true
    }
    val visibility = status.visibility
    if (visibility == Visibilities.Private || !visibility.visibleFromPackage(classPackage, packageName)) return false
    if (visibility == Visibilities.Internal) {
        return classSymbol.moduleData.canSeeInternalsOf(moduleData)
    }
    return true
}
