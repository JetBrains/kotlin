/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallElement

internal class FirLightSimpleAnnotation(
    private val fqName: String?,
    parent: PsiElement
) : FirLightAbstractAnnotation(parent) {
    override val kotlinOrigin: KtCallElement? = null

    override fun getQualifiedName(): String? = fqName

    override fun getName(): String? = fqName

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightSimpleAnnotation && fqName == other.fqName && parent == other.parent)

    override fun hashCode(): Int = fqName.hashCode()
}
