/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

private val _allElementsById = arrayOfNulls<BirElementClass<*>>(BirMetadata.allElements.maxOf { it.id } + 1).also { array ->
    for (element in BirMetadata.allElements) {
        array[element.id] = element
    }
}
private val _allElementsByJavaClass = BirMetadata.allElements.associateBy { it.javaClass }

internal val BirMetadata.allElementsById: Array<BirElementClass<*>?>
    get() = _allElementsById

val BirMetadata.allElementsByJavaClass: Map<Class<out BirElement>, BirElementClass<*>>
    get() = _allElementsByJavaClass