/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.java.source

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory

internal class JavaElementPsiSourceWithSmartPointer<PSI : PsiElement>(
    val pointer: SmartPsiElementPointer<PSI>,
    override val factory: JavaElementSourceFactory,
) : JavaElementPsiSource<PSI>() {
    override val psi: PSI
        get() = pointer.element ?: error("Cannot restore a PsiElement from $pointer")

    // Only compare pointers here to avoid an underlying PCE (see KT-59445).
    override fun equals(other: Any?): Boolean =
        other === this || other is JavaElementPsiSourceWithSmartPointer<*> && pointer == other.pointer

    override fun hashCode(): Int = pointer.hashCode()
}
