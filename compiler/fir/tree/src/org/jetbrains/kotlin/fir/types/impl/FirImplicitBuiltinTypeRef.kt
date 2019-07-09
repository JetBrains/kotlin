/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId

sealed class FirImplicitBuiltinTypeRef(
    session: FirSession,
    psi: PsiElement?,
    val id: ClassId
) : FirResolvedTypeRef, FirAbstractElement(session, psi) {
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()

    override val type: ConeKotlinType = ConeClassTypeImpl(ConeClassLikeLookupTagImpl(id), emptyArray(), false)
}

class FirImplicitUnitTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.Unit)

class FirImplicitAnyTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.Any)

class FirImplicitEnumTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.Enum)

class FirImplicitAnnotationTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.Annotation)

class FirImplicitBooleanTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.Boolean)

class FirImplicitNothingTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.Nothing)

class FirImplicitStringTypeRef(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinTypeRef(session, psi, StandardClassIds.String)

