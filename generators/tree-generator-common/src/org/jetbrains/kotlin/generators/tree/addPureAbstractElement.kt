/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

val ImplementationKindOwner.needPureAbstractElement: Boolean
    get() = !(kind?.isInterface ?: false) &&
            allParents.none { it.kind == ImplementationKind.AbstractClass || it.kind == ImplementationKind.SealedClass }

fun addPureAbstractElement(elements: List<AbstractElement<*, *, *>>, pureAbstractElement: ClassRef<*>) {
    for (el in elements) {
        if (el.needPureAbstractElement) {
            el.otherParents.add(pureAbstractElement)
        }
    }
}