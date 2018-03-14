/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.types.KotlinType

class FirUnitType(
    override val session: FirSession,
    override val psi: PsiElement?
) : FirResolvedType {
    override val type: KotlinType
        get() = DefaultBuiltIns.Instance.unitType

    override val isNullable = false

    override val annotations: List<FirAnnotationCall>
        get() = emptyList()
}