/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.types.FirType

class FirPrimaryConstructorImpl(
    session: FirSession,
    psi: PsiElement?,
    visibility: Visibility,
    isExpect: Boolean,
    isActual: Boolean,
    delegatedSelfType: FirType,
    delegatedConstructor: FirDelegatedConstructorCall?
) : FirConstructorImpl(session, psi, visibility, isExpect, isActual, delegatedSelfType, delegatedConstructor, body = null)