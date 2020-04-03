/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.RelationToType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirEffectiveVisibility.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection

fun firEffectiveVisibility(
    session: FirSession,
    visibility: Visibility,
    declaration: FirMemberDeclaration?
) =
    visibility.forVisibility(session, declaration)

fun FirRegularClass.firEffectiveVisibility(session: FirSession, checkPublishedApi: Boolean = false) =
    firEffectiveVisibility(session, emptySet(), checkPublishedApi)

fun Visibility.firEffectiveVisibility(session: FirSession, declaration: FirMemberDeclaration?): FirEffectiveVisibility =
    firEffectiveVisibility(session, normalize(), declaration)

fun ConeKotlinType.leastPermissiveDescriptor(session: FirSession, base: FirEffectiveVisibility): DeclarationWithRelation? =
    dependentDeclarations(session).leastPermissive(session, base)

fun FirMemberDeclaration.firEffectiveVisibility(
    session: FirSession, visibility: Visibility = this.visibility, checkPublishedApi: Boolean = false
): FirEffectiveVisibility =
    lowerBound(
        visibility.firEffectiveVisibility(session, this),
        this.containingClass(session)?.firEffectiveVisibility(session, checkPublishedApi) ?: Public
    )

private fun lowerBound(first: FirEffectiveVisibility, second: FirEffectiveVisibility) =
    first.lowerBound(second)

private fun FirMemberDeclaration.containingClass(session: FirSession): FirRegularClass? {
    val classId = when (this) {
        is FirRegularClass -> symbol.classId.outerClassId
        is FirCallableMemberDeclaration<*> -> symbol.callableId.classId
        else -> null
    } ?: return null
    if (classId.isLocal) return null
    return session.firSymbolProvider.getClassLikeSymbolByFqName(classId)?.fir as? FirRegularClass
}

private fun Visibility.forVisibility(
    session: FirSession, declaration: FirMemberDeclaration?
): FirEffectiveVisibility =
    when (this) {
        Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS, Visibilities.INVISIBLE_FAKE -> Private
        Visibilities.PROTECTED -> Protected(declaration?.containingClass(session))
        Visibilities.INTERNAL -> Internal
        Visibilities.PUBLIC -> Public
        Visibilities.LOCAL -> Local
        // NB: visibility must be already normalized here, so e.g. no JavaVisibilities are possible at this point
        else -> throw AssertionError("Visibility $name is not allowed in forVisibility")
    }

class DeclarationWithRelation internal constructor(val declaration: FirMemberDeclaration, private val relation: RelationToType) {
    fun firEffectiveVisibility(session: FirSession) =
        declaration.visibility.firEffectiveVisibility(session, declaration)

    override fun toString(): String {
        val name = when (declaration) {
            is FirRegularClass -> declaration.name
            is FirSimpleFunction -> declaration.name
            is FirVariable<*> -> declaration.name
            else -> "<anonymous>"
        }
        return "$relation $name"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeclarationWithRelation) return false
        if (declaration != other.declaration) return false
        if (relation != other.relation) return false
        return true
    }

    override fun hashCode(): Int {
        var result = declaration.hashCode()
        result = 31 * result + relation.hashCode()
        return result
    }
}

private fun FirMemberDeclaration.dependentDeclarations(session: FirSession, ownRelation: RelationToType): Set<DeclarationWithRelation> =
    setOf(DeclarationWithRelation(this, ownRelation)) +
            (this.containingClass(session)?.dependentDeclarations(session, ownRelation.containerRelation()) ?: emptySet())

private fun FirRegularClass.firEffectiveVisibility(
    session: FirSession, classes: Set<FirRegularClass>, checkPublishedApi: Boolean
): FirEffectiveVisibility =
    if (this in classes) Public
    else with(this.containingClass(session)) {
        lowerBound(
            visibility.firEffectiveVisibility(session, this@firEffectiveVisibility),
            this?.firEffectiveVisibility(session, classes + this@firEffectiveVisibility, checkPublishedApi) ?: Public
        )
    }

// Should collect all dependent classifier descriptors, to get verbose diagnostic
private fun ConeKotlinType.dependentDeclarations(session: FirSession) =
    dependentDeclarations(session, emptySet(), RelationToType.CONSTRUCTOR)

private fun ConeKotlinType.dependentDeclarations(
    session: FirSession, types: Set<ConeKotlinType>, ownRelation: RelationToType
): Set<DeclarationWithRelation> {
    if (this in types) return emptySet()
    val classLikeType = this as? ConeClassLikeType ?: return emptySet()
    val lookupTag = classLikeType.lookupTag
    val classSymbol = lookupTag.toSymbol(session) ?: return emptySet()
    val ownDependent = (classSymbol.fir as? FirMemberDeclaration)?.dependentDeclarations(session, ownRelation) ?: emptySet()
    val argumentDependent = classLikeType.typeArguments.mapNotNull {
        (it as? ConeKotlinTypeProjection)?.type?.dependentDeclarations(session, types + this, RelationToType.ARGUMENT)
    }.flatten()
    return ownDependent + argumentDependent
}

private fun Set<DeclarationWithRelation>.leastPermissive(session: FirSession, base: FirEffectiveVisibility): DeclarationWithRelation? {
    for (declarationWithRelation in this) {
        val currentVisibility = declarationWithRelation.firEffectiveVisibility(session)
        when (currentVisibility.relation(base)) {
            Permissiveness.LESS, Permissiveness.UNKNOWN -> {
                return declarationWithRelation
            }
            else -> {
            }
        }
    }
    return null
}

