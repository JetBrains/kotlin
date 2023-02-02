/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

/**
 * Application of annotation to some declaration, type, or as argument inside another annotation.
 *
 * Some examples:
 * - For declarations: `@Deprecated("Should not be used") fun foo(){}`
 * - For types: `fun foo(x: List<@A Int>){}`
 * - Inside another annotation (`B` is annotation here): `@A(B()) fun foo(){}
 *
 * @see KtAnnotationApplicationInfo
 * @see KtAnnotationApplicationWithArgumentsInfo
 */
public sealed interface KtAnnotationApplication {
    /**
     * The [ClassId] of applied annotation. [ClassId] is a fully qualified name on annotation class.
     */
    public val classId: ClassId?

    /**
     * [com.intellij.psi.PsiElement] which was used to apply annotation to declaration/type.
     *
     * Present only for declarations from sources. For declarations from other places (libraries, stdlib) it's `null`
     */
    public val psi: KtCallElement?

    /**
     * [AnnotationUseSiteTarget] to which annotation was applied. May be not-null only for annotation applications for declarations.
     *
     * See more details in [Kotlin Documentation](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets) for more information about annotation targets.
     */
    public val useSiteTarget: AnnotationUseSiteTarget?

    /**
     * This property can be used to optimize some argument processing logic.
     * For example, if you have [KtAnnotationApplicationInfo] from [KtAnnotated.annotationInfos] and [isCallWithArguments] is **false**,
     * then you can avoid [KtAnnotated.annotationsByClassId] call,
     * because effectively you already have all necessary information in [KtAnnotationApplicationInfo]
     */
    public val isCallWithArguments: Boolean

    /**
     * An index of the annotation in an owner. `null` when annotation is used as an argument of other annotations
     */
    public val index: Int?
}
