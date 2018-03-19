/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name

sealed class FirBuiltinType(
    final override val session: FirSession,
    final override val psi: PsiElement?,
    val name: String
) : FirResolvedType {
    final override val type: ConeKotlinType = ConeClassTypeImpl(
        UnambiguousFqName(
            KOTLIN_PACKAGE_FQ_NAME,
            FqName.topLevel(Name.identifier(name))
        ), emptyList()
    )

    final override val isNullable = false

    final override val annotations: List<FirAnnotationCall>
        get() = emptyList()

    companion object {
        private val KOTLIN_PACKAGE_FQ_NAME = FqNameUnsafe.topLevel(Name.identifier("kotlin"))
    }
}

class FirUnitType(
    session: FirSession,
    psi: PsiElement?
) : FirBuiltinType(session, psi, "Unit")