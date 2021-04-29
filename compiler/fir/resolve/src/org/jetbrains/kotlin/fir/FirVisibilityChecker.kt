/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.ReceiverValue
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface FirModuleVisibilityChecker : FirSessionComponent {
    fun isInFriendModule(declaration: FirMemberDeclaration): Boolean
}

abstract class FirVisibilityChecker : FirSessionComponent {
    @NoMutableState
    object Default : FirVisibilityChecker() {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            symbol: AbstractFirBasedSymbol<*>,
            useSiteFile: FirFile,
            containingDeclarations: List<FirDeclaration>,
            dispatchReceiver: ReceiverValue?,
            session: FirSession
        ): Boolean {
            return true
        }
    }

    fun <T> isVisible(
        declaration: T,
        candidate: Candidate
    ): Boolean where T : FirMemberDeclaration, T : FirSymbolOwner<*> {
        if (declaration is FirCallableDeclaration<*> && (declaration.isIntersectionOverride || declaration.isSubstitutionOverride)) {
            @Suppress("UNCHECKED_CAST")
            return isVisible(declaration.originalIfFakeOverride() as T, candidate)
        }

        val callInfo = candidate.callInfo
        val useSiteFile = callInfo.containingFile
        val containingDeclarations = callInfo.containingDeclarations
        val session = callInfo.session

        return isVisible(declaration, session, useSiteFile, containingDeclarations, candidate.dispatchReceiverValue)
    }

    fun <T> isVisible(
        declaration: T,
        session: FirSession,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?
    ): Boolean where T : FirMemberDeclaration, T : FirSymbolOwner<*> {
        val provider = session.firProvider
        val symbol = declaration.symbol
        return when (declaration.visibility) {
            Visibilities.Internal -> {
                declaration.moduleData == session.moduleData || session.moduleVisibilityChecker?.isInFriendModule(declaration) == true
            }
            Visibilities.Private, Visibilities.PrivateToThis -> {
                val ownerId = symbol.getOwnerId()
                if (declaration.moduleData == session.moduleData) {
                    when {
                        ownerId == null -> {
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
                            canSeePrivateMemberOf(containingDeclarations, ownerId, session)
                        }
                    }
                } else {
                    declaration is FirSimpleFunction && declaration.isAllowedToBeAccessedFromOutside()
                }
            }

            Visibilities.Protected -> {
                val ownerId = symbol.getOwnerId()
                ownerId != null && canSeeProtectedMemberOf(containingDeclarations, dispatchReceiver, ownerId, session)
            }

            else -> platformVisibilityCheck(
                declaration.visibility,
                symbol,
                useSiteFile,
                containingDeclarations,
                dispatchReceiver,
                session
            )
        }
    }

    protected abstract fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        symbol: AbstractFirBasedSymbol<*>,
        useSiteFile: FirFile,
        containingDeclarations: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?,
        session: FirSession
    ): Boolean

    private fun canSeePrivateMemberOf(
        containingDeclarationOfUseSite: List<FirDeclaration>,
        ownerId: ClassId,
        session: FirSession
    ): Boolean {
        ownerId.ownerIfCompanion(session)?.let { companionOwnerClassId ->
            return canSeePrivateMemberOf(containingDeclarationOfUseSite, companionOwnerClassId, session)
        }

        for (declaration in containingDeclarationOfUseSite) {
            if (declaration !is FirClass<*>) continue
            val boundSymbol = declaration.symbol
            if (boundSymbol.classId.isSame(ownerId)) {
                return true
            }
        }

        return false
    }

    // 'local' isn't taken into account here
    private fun ClassId.isSame(other: ClassId): Boolean =
        packageFqName == other.packageFqName && relativeClassName == other.relativeClassName

    private fun ClassId.ownerIfCompanion(session: FirSession): ClassId? {
        if (outerClassId == null || isLocal) return null
        val ownerSymbol = session.symbolProvider.getClassLikeSymbolByFqName(this) as? FirRegularClassSymbol

        return outerClassId.takeIf { ownerSymbol?.fir?.isCompanion == true }
    }

    private fun canSeeProtectedMemberOf(
        containingUseSiteClass: FirClass<*>,
        dispatchReceiver: ReceiverValue?,
        ownerId: ClassId, session: FirSession
    ): Boolean {
        dispatchReceiver?.ownerIfCompanion(session)?.let { companionOwnerClassId ->
            if (containingUseSiteClass.isSubClass(companionOwnerClassId, session)) return true
        }

        // TODO: Add check for receiver, see org.jetbrains.kotlin.descriptors.Visibility#doesReceiverFitForProtectedVisibility
        return containingUseSiteClass.isSubClass(ownerId, session)
    }

    private fun FirClass<*>.isSubClass(ownerId: ClassId, session: FirSession): Boolean {
        if (classId.isSame(ownerId)) return true

        return lookupSuperTypes(this, lookupInterfaces = true, deep = true, session).any { superType ->
            (superType as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag?.classId?.isSame(ownerId) == true
        }
    }

    private fun ReceiverValue?.ownerIfCompanion(session: FirSession): ClassId? =
        (this?.type as? ConeClassLikeType)?.lookupTag?.classId?.ownerIfCompanion(session)

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
        ownerId: ClassId, session: FirSession
    ): Boolean {
        if (canSeePrivateMemberOf(containingDeclarationOfUseSite, ownerId, session)) return true

        for (containingDeclaration in containingDeclarationOfUseSite) {
            if (containingDeclaration !is FirClass<*>) continue
            val boundSymbol = containingDeclaration.symbol
            if (canSeeProtectedMemberOf(boundSymbol.fir, dispatchReceiver, ownerId, session)) return true
        }

        return false
    }

    protected fun AbstractFirBasedSymbol<*>.packageFqName(): FqName {
        return when (this) {
            is FirClassLikeSymbol<*> -> classId.packageFqName
            is FirCallableSymbol<*> -> callableId.packageName
            else -> error("No package fq name for $this")
        }
    }
}

val FirSession.moduleVisibilityChecker: FirModuleVisibilityChecker? by FirSession.nullableSessionComponentAccessor()
val FirSession.visibilityChecker: FirVisibilityChecker by FirSession.sessionComponentAccessor()

fun AbstractFirBasedSymbol<*>.getOwnerId(): ClassId? {
    return when (this) {
        is FirClassLikeSymbol<*> -> {
            val ownerId = classId.outerClassId
            if (classId.isLocal) {
                ownerId?.asLocal() ?: classId
            } else {
                ownerId
            }
        }
        is FirCallableSymbol<*> -> containingClass()?.classId
        else -> error("Unsupported owner search for ${fir.javaClass}: ${fir.render()}")
    }
}

private fun ClassId.asLocal(): ClassId = ClassId(packageFqName, relativeClassName, true)
