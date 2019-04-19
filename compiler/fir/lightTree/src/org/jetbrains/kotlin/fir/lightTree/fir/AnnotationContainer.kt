/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement

class AnnotationContainer(
    session: FirSession,
    psi: PsiElement? = null
) : FirAbstractAnnotatedElement(session, psi) {
    constructor(session: FirSession, annotations: List<FirAnnotationCall>) : this(session, null) {
        super.annotations += annotations
    }
}