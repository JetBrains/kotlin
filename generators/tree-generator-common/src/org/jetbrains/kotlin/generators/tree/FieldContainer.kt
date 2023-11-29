/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

interface FieldContainer<out Field : AbstractField<*>> {

    /**
     * All the fields of this element, including the fields of all its parents.
     */
    val allFields: List<Field>

    operator fun get(fieldName: String): Field?

    val hasAcceptMethod: Boolean
        get() = false

    val hasTransformMethod: Boolean
        get() = false

    val hasAcceptChildrenMethod: Boolean
        get() = false

    val hasTransformChildrenMethod: Boolean
        get() = false

    /**
     * Allows to override the order in which the specified children will be visited in `acceptChildren`/`transformChildren` methods.
     */
    val childrenOrderOverride: List<String>?
        get() = null

    /**
     * The fields on which to run the visitor in generated `acceptChildren` methods.
     */
    val walkableChildren: List<Field>
        get() = allFields
            .filter { it.containsElement && !it.withGetter && it.isChild }
            .reorderFieldsIfNecessary(childrenOrderOverride)

    /**
     * The fields on which to run the transformer in generated `transformChildren` methods.
     */
    val transformableChildren: List<Field>
        get() = walkableChildren.filter { it.isMutable || it is ListField }
}
