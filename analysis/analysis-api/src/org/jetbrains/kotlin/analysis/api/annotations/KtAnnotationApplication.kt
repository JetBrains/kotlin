/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallElement

/**
 * Application of annotation to some declaration, type, or as argument inside other annotation.
 *
 * Some examples:
 * - For declarations: `@Deprecated("Should not be used") fun foo(){}`
 * - For types: `fun foo(x: List<@A Int>){}`
 * - Inside other annotation (`B` is annotation here): `@A(B()) fun foo(){}
 */
public data class KtAnnotationApplication(
    /**
     * The [ClassId] of applied annotation. [ClassId] is a fully qualified name on annotation class.
     */
    public val classId: ClassId?,

    /**
     * PsiElement which was used to apply annotation to declaration/type.
     *
     * Present only for declarations from sources. For declarations from other places (libraries, stdlib) it's `null`
     */
    public val psi: KtCallElement?,

    /**
     * [AnnotationUseSiteTarget] to which annotation was applied. May be not-null only for annotation applications for declarations.
     *
     * See in more details in [Kotlin Documentation](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets) for more information about annotation targets.
     */
    public val useSiteTarget: AnnotationUseSiteTarget?,

    /**
     * A list of annotation arguments which were applied when constructing annotation. Every argument is [KtAnnotationValue]
     */
    public val arguments: List<KtNamedAnnotationValue>,
)