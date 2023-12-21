/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase

abstract class BirElementClass(val javaClass: Class<*>, val id: Int, val hasImplementation: Boolean)

internal object BirElementClassCache : ClassValue<BirElementClass>() {
    override fun computeValue(type: Class<*>): BirElementClass {
        var elementClass = type
        if (elementClass.simpleName.endsWith("Impl") || elementClass.simpleName.startsWith("BirLazy")) {
            elementClass = elementClass.superclass
                .takeUnless { it == BirImplElementBase::class.java || it == BirLazyElementBase::class.java }
                ?: elementClass.interfaces.single { it.simpleName.startsWith("Bir") }
        }

        return elementClass.declaredClasses
            .find { it.simpleName == "Companion" }!!
            .kotlin.objectInstance as BirElementClass
    }
}
