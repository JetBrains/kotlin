/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class FirVisibilityChecker : FirSessionComponent {
    object Default : FirVisibilityChecker() {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            symbol: AbstractFirBasedSymbol<*>,
            useSiteFile: FirFile,
            ownerId: ClassId?,
            containingDeclarations: List<FirDeclaration>,
            candidate: Candidate,
            session: FirSession
        ): Boolean {
            return true
        }
    }

    fun isVisible(
        declaration: FirMemberDeclaration,
        symbol: AbstractFirBasedSymbol<*>,
        candidate: Candidate
    ): Boolean {
        val callInfo = candidate.callInfo
        val useSiteFile = callInfo.containingFile
        val containingDeclarations = callInfo.containingDeclarations
        val session = callInfo.session
        val provider = session.firProvider
        val ownerId = symbol.getOwnerId()

        return when (declaration.visibility) {
            Visibilities.Internal -> {
                declaration.session == callInfo.session
            }
            Visibilities.Private, Visibilities.PrivateToThis -> {
                if (declaration.session == callInfo.session) {
                    if (ownerId == null || declaration is FirConstructor && declaration.isFromSealedClass) {
                        val candidateFile = when (symbol) {
                            is FirClassLikeSymbol<*> -> provider.getFirClassifierContainerFileIfAny(symbol)
                            is FirCallableSymbol<*> -> provider.getFirCallableContainerFile(symbol)
                            else -> null
                        }
                        // Top-level: visible in file
                        candidateFile == useSiteFile
                    } else {
                        // Member: visible inside parent class, including all its member classes
                        canSeePrivateMemberOf(containingDeclarations, ownerId, session)
                    }
                } else {
                    declaration is FirSimpleFunction && declaration.isAllowedToBeAccessedFromOutside()
                }
            }

            Visibilities.Protected -> {
                ownerId != null && canSeeProtectedMemberOf(containingDeclarations, candidate.dispatchReceiverValue, ownerId, session)
            }

            else -> platformVisibilityCheck(
                declaration.visibility,
                symbol,
                useSiteFile,
                ownerId,
                containingDeclarations,
                candidate,
                session
            )
        }
    }

    protected abstract fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        symbol: AbstractFirBasedSymbol<*>,
        useSiteFile: FirFile,
        ownerId: ClassId?,
        containingDeclarations: List<FirDeclaration>,
        candidate: Candidate,
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
        val ownerSymbol = session.firSymbolProvider.getClassLikeSymbolByFqName(this) as? FirRegularClassSymbol

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

    private fun AbstractFirBasedSymbol<*>.getOwnerId(): ClassId? {
        return when (this) {
            is FirClassLikeSymbol<*> -> {
                val ownerId = classId.outerClassId
                if (classId.isLocal) {
                    ownerId?.asLocal() ?: classId
                } else {
                    ownerId
                }
            }
            is FirCallableSymbol<*> -> callableId.classId
            else -> error("Unsupported owner search for ${fir.javaClass}: ${fir.render()}")
        }
    }

    private fun ClassId.asLocal(): ClassId = ClassId(packageFqName, relativeClassName, true)

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

val FirSession.visibilityChecker: FirVisibilityChecker by FirSession.sessionComponentAccessor()
