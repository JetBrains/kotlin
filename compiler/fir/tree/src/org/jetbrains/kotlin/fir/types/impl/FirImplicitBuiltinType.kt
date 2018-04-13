/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirImplicitType
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

sealed class FirImplicitBuiltinType(
    override val session: FirSession,
    override val psi: PsiElement?,
    val name: Name
) : FirImplicitType {
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()

    constructor(session: FirSession, psi: PsiElement?, name: FqNameUnsafe) : this(session, psi, name.shortName())
}

class FirImplicitUnitType(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinType(session, psi, KotlinBuiltIns.FQ_NAMES.unit)

class FirImplicitAnyType(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinType(session, psi, KotlinBuiltIns.FQ_NAMES.any)

class FirImplicitEnumType(
    session: FirSession,
    psi: PsiElement?
) : FirImplicitBuiltinType(session, psi, KotlinBuiltIns.FQ_NAMES._enum)