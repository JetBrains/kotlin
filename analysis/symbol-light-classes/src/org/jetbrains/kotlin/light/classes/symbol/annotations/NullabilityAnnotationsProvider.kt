/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.utils.NullabilityAnnotation
import org.jetbrains.kotlin.light.classes.symbol.utils.asAnnotationQualifier

internal class NullabilityAnnotationsProvider(private val lazyNullabilityType: Lazy<NullabilityAnnotation>) : AdditionalAnnotationsProvider {
    constructor(initializer: () -> NullabilityAnnotation) : this(lazyPub(initializer))

    override fun addAllAnnotations(
        currentRawAnnotations: MutableList<in PsiAnnotation>,
        foundQualifiers: MutableSet<String>,
        owner: PsiElement
    ) {
        val qualifier = lazyNullabilityType.value.asAnnotationQualifier ?: return
        addSimpleAnnotationIfMissing(qualifier, currentRawAnnotations, foundQualifiers, owner)
    }
}
