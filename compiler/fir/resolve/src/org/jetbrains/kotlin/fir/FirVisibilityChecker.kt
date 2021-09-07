/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ExpressionReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.ReceiverValue
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class FirModuleVisibilityChecker : FirSessionComponent {
    abstract fun <T> isInFriendModule(declaration: T): Boolean where T : FirMemberDeclaration, T : FirDeclaration

    class Standard(val session: FirSession) : FirModuleVisibilityChecker() {
        override fun <T> isInFriendModule(declaration: T): Boolean where T : FirMemberDeclaration, T : FirDeclaration {
            val useSiteModuleData = session.moduleData
            val declarationModuleData = declaration.moduleData
            return useSiteModuleData == declarationModuleData || declarationModuleData in useSiteModuleData.friendDependencies
        }
    }
}

abstract class FirVisibilityChecker : FirSessionComponent {
    @NoMutableState
    object Default : FirVisibilityChecker() {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            symbol: FirBasedSymbol<*>,
            useSiteFile: FirFile,
            containingDeclarations: List<FirDeclaration>,
            dispatchReceiver: ReceiverValue?,
            session: FirSession,
            isCallToPropertySetter: Boolean,
        ): Boolean {
            return true
        }
    }

    private fun FirMemberDeclaration.getBackingFieldIfApplicable(): FirBackingField? {
        val field = this.safeAs<FirProperty>()?.getExplicitBackingField()
            ?: return null

        // This check prevents resolving protected and
        // public fields.
        if (
            field.visibility == Visibilities.PrivateToThis ||
            field.visibility == Visibilities.Private ||
            field.visibility == Visibilities.Internal
        ) {
            return field
        }

        return null
    }

    fun isVisible(
        declaration: FirMemberDeclaration,
        candidate: Candidate
    ): Boolean {
        if (declaration is FirCallableDeclaration && (declaration.isIntersectionOverride || declaration.isSubstitutionOverride)) {
            @Suppress("UNCHECKED_CAST")
            return isVisible(declaration.originalIfFakeOverride() as FirMemberDeclaration, candidate)
        }

        val callInfo = candidate.callInfo
        val useSiteFile = callInfo.containingFile
        val containingDeclarations = callInfo.containingDeclarations
        val session = callInfo.session

        // We won't resolve into the backing field
        // in the first place, if it's not accessible.
        if (declaration is FirBackingField) {
            return true
        }

        val visible = isVisible(
            declaration,
            session,
            useSiteFile,
            containingDeclarations,
            candidate.dispatchReceiverValue,
            candidate.callInfo.callSite is FirVariableAssignment
        )
        val backingField = declaration.getBackingFieldIfApplicable()

        if (visible && backingField != null) {
            candidate.hasVisibleBackingField = isVisible(
                backingField,
                session,
                useSiteFile,
                containingDeclarations,
                candidate.dispatchReceiverValue,
                candidate.callInfo.callSite is FirVariableAssignment,
            )
        }

        return visible
    }

    fun isVisible(
        declaration: FirMemberDeclaration,
        session: FirSession,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?,
        isCallToPropertySetter: Boolean = false,
    ): Boolean {
        require(declaration is FirDeclaration)
        val provider = session.firProvider
        val symbol = declaration.symbol
        return when (declaration.visibility) {
            Visibilities.Internal -> {
                declaration.moduleData == session.moduleData || session.moduleVisibilityChecker?.isInFriendModule(declaration) == true
            }
            Visibilities.Private, Visibilities.PrivateToThis -> {
                val ownerLookupTag = symbol.getOwnerLookupTag()
                if (declaration.moduleData == session.moduleData) {
                    when {
                        ownerLookupTag == null -> {
                            val candidateFile = when (symbol) {
                                is FirSyntheticFunctionSymbol -> {
                                    // SAM case
                                    val classId = ClassId(symbol.callableId.packageName, symbol.callableId.callableName)
                                    provider.getFirClassifierContainerFile(classId)
                                }
                                is FirClassLikeSymbol<*> -> provider.getFirClassifierContainerFileIfAny(symbol)
                                is FirCallableSymbol<*> -> provider.getFirCallableContainerFile(symbol)
                                else -> null
                            }
                            // Top-level: visible in file
                            candidateFile == useSiteFile
                        }
                        declaration is FirConstructor && declaration.isFromSealedClass -> {
                            // Sealed class constructor: visible in same package
                            declaration.symbol.callableId.packageName == useSiteFile.packageFqName
                        }
                        else -> {
                            // Member: visible inside parent class, including all its member classes
                            canSeePrivateMemberOf(containingDeclarations, ownerLookupTag, session)
                        }
                    }
                } else {
                    declaration is FirSimpleFunction && declaration.isAllowedToBeAccessedFromOutside()
                }
            }

            Visibilities.Protected -> {
                val ownerId = symbol.getOwnerLookupTag()
                ownerId != null && canSeeProtectedMemberOf(
                    containingDeclarations, dispatchReceiver, ownerId, session,
                    isVariableOrNamedFunction = symbol is FirVariableSymbol || symbol is FirNamedFunctionSymbol
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
            )
        }
    }

    protected abstract fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        symbol: FirBasedSymbol<*>,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?,
        session: FirSession,
        isCallToPropertySetter: Boolean,
    ): Boolean

    private fun canSeePrivateMemberOf(
        containingDeclarationOfUseSite: List<FirDeclaration>,
        ownerLookupTag: ConeClassLikeLookupTag,
        session: FirSession
    ): Boolean {
        ownerLookupTag.ownerIfCompanion(session)?.let { companionOwnerLookupTag ->
            return canSeePrivateMemberOf(containingDeclarationOfUseSite, companionOwnerLookupTag, session)
        }

        for (declaration in containingDeclarationOfUseSite) {
            if (declaration !is FirClass) continue
            val boundSymbol = declaration.symbol
            if (boundSymbol.classId.isSame(ownerLookupTag.classId)) {
                return true
            }
        }

        return false
    }

    // 'local' isn't taken into account here
    private fun ClassId.isSame(other: ClassId): Boolean =
        packageFqName == other.packageFqName && relativeClassName == other.relativeClassName

    private fun ConeClassLikeLookupTag.ownerIfCompanion(session: FirSession): ConeClassLikeLookupTag? {
        if (classId.isLocal) return null
        val outerClassId = classId.outerClassId ?: return null
        val ownerSymbol = toSymbol(session) as? FirRegularClassSymbol

        if (ownerSymbol?.fir?.isCompanion == true) {
            return ConeClassLikeLookupTagImpl(outerClassId)
        }
        return null
    }

    private fun canSeeProtectedMemberOf(
        containingUseSiteClass: FirClass,
        dispatchReceiver: ReceiverValue?,
        ownerLookupTag: ConeClassLikeLookupTag,
        session: FirSession,
        isVariableOrNamedFunction: Boolean
    ): Boolean {
        dispatchReceiver?.ownerIfCompanion(session)?.let { companionOwnerLookupTag ->
            if (containingUseSiteClass.isSubClass(companionOwnerLookupTag, session)) return true
        }

        return when {
            !containingUseSiteClass.isSubClass(ownerLookupTag, session) -> false
            isVariableOrNamedFunction -> doesReceiverFitForProtectedVisibility(dispatchReceiver, containingUseSiteClass, session)
            else -> true
        }
    }

    private fun doesReceiverFitForProtectedVisibility(
        dispatchReceiver: ReceiverValue?,
        containingUseSiteClass: FirClass,
        session: FirSession
    ): Boolean {
        if (dispatchReceiver == null) return true
        var dispatchReceiverType = dispatchReceiver.type
        if (dispatchReceiver is ExpressionReceiverValue) {
            val explicitReceiver = dispatchReceiver.explicitReceiver
            if (explicitReceiver is FirPropertyAccessExpression && explicitReceiver.calleeReference is FirSuperReference) {
                // Special 'super' case: type of this, not of super, should be taken for the check below
                dispatchReceiverType = explicitReceiver.dispatchReceiver.typeRef.coneType
            }
        }
        val typeCheckerState = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )
        return AbstractTypeChecker.isSubtypeOf(
            typeCheckerState, dispatchReceiverType.fullyExpandedType(session), containingUseSiteClass.typeWithStarProjections()
        )
    }

    private fun FirClass.isSubClass(ownerLookupTag: ConeClassLikeLookupTag, session: FirSession): Boolean {
        if (classId.isSame(ownerLookupTag.classId)) return true

        return lookupSuperTypes(this, lookupInterfaces = true, deep = true, session).any { superType ->
            (superType as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag?.classId?.isSame(ownerLookupTag.classId) == true
        }
    }

    private fun ReceiverValue?.ownerIfCompanion(session: FirSession): ConeClassLikeLookupTag? =
        (this?.type as? ConeClassLikeType)?.lookupTag?.ownerIfCompanion(session)

    // monitorEnter/monitorExit are the only functions which are accessed "illegally" (see kotlin/util/Synchronized.kt).
    // Since they are intrinsified in the codegen, FIR should treat it as visible.
    private fun FirSimpleFunction.isAllowedToBeAccessedFromOutside(): Boolean {
        if (!isFromLibrary) return false
        val packageName = symbol.callableId.packageName.asString()
        val name = name.asString()
        return packageName == "kotlin.jvm.internal.unsafe" &&
                (name == "monitorEnter" || name == "monitorExit")
    }

    protected fun canSeeProtectedMemberOf(
        containingDeclarationOfUseSite: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?,
        ownerLookupTag: ConeClassLikeLookupTag,
        session: FirSession,
        isVariableOrNamedFunction: Boolean
    ): Boolean {
        if (canSeePrivateMemberOf(containingDeclarationOfUseSite, ownerLookupTag, session)) return true

        for (containingDeclaration in containingDeclarationOfUseSite) {
            if (containingDeclaration !is FirClass) continue
            val boundSymbol = containingDeclaration.symbol
            if (canSeeProtectedMemberOf(boundSymbol.fir, dispatchReceiver, ownerLookupTag, session, isVariableOrNamedFunction)) return true
        }

        return false
    }

    protected fun FirBasedSymbol<*>.packageFqName(): FqName {
        return when (this) {
            is FirClassLikeSymbol<*> -> classId.packageFqName
            is FirCallableSymbol<*> -> callableId.packageName
            else -> error("No package fq name for $this")
        }
    }
}

val FirSession.moduleVisibilityChecker: FirModuleVisibilityChecker? by FirSession.nullableSessionComponentAccessor()
val FirSession.visibilityChecker: FirVisibilityChecker by FirSession.sessionComponentAccessor()

fun FirBasedSymbol<*>.getOwnerLookupTag(): ConeClassLikeLookupTag? {
    return when (this) {
        is FirBackingFieldSymbol -> fir.propertySymbol.getOwnerLookupTag()
        is FirClassLikeSymbol<*> -> {
            if (classId.isLocal) {
                (fir as? FirRegularClass)?.containingClassForLocal()
            } else {
                val ownerId = classId.outerClassId
                ownerId?.let { ConeClassLikeLookupTagImpl(it) }
            }
        }
        is FirCallableSymbol<*> -> containingClass()
        else -> error("Unsupported owner search for ${fir.javaClass}: ${fir.render()}")
    }
}
