/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.name.ClassId

sealed class FirImplicitBuiltinTypeRef(
    override val session: FirSession,
    override val psi: PsiElement?,
    val id: ClassId
) : FirImplicitTypeRef {
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()
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
