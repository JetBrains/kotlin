/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

interface FieldContainer {
    val allFields: List<AbstractField>
    operator fun get(fieldName: String): AbstractField?

    val hasAcceptMethod: Boolean
        get() = false

    val hasTransformMethod: Boolean
        get() = false

    val hasAcceptChildrenMethod: Boolean
        get() = false

    val hasTransformChildrenMethod: Boolean
        get() = false

    /**
     * The fields on which to run the visitor in generated `acceptChildren` methods.
     */
    val walkableChildren: List<AbstractField>
        get() = allFields.filter { it.containsElement && !it.withGetter && it.needAcceptAndTransform }

    /**
     * The fields on which to run the transformer in generated `transformChildren` methods.
     */
    val transformableChildren: List<AbstractField>
        get() = walkableChildren.filter { it.isMutable || it is ListField }
}