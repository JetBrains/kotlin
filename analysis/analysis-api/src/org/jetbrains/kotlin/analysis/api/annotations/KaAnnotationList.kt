/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.name.ClassId

/**
 * A list of annotations applied to an entity.
 *
 * Annotation owners usually implement [KaAnnotated].
 */
public interface KaAnnotationList : List<KaAnnotation>, KaLifetimeOwner {
    /**
     * Checks if the entity contains an annotation with the specified [classId].
     */
    public operator fun contains(classId: ClassId): Boolean

    /**
     * Returns the list of annotations with the specified [classId]. The same type of annotation may occur multiple times if it is
     * [repeatable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.annotation/-repeatable/).
     *
     * To check if an annotation is present, use [contains] instead.
     */
    public operator fun get(classId: ClassId): List<KaAnnotation>

    /**
     * The list of [ClassId]s of all annotations, in their original order. If the entity has [repeatable](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.annotation/-repeatable/)
     * annotations, [classIds] may contain the same class ID multiple times.
     *
     * To check if an annotation is present, use [contains] instead.
     */
    public val classIds: Collection<ClassId>
}
