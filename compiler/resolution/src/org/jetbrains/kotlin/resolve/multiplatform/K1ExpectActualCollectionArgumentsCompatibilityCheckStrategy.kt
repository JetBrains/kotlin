/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform


sealed class K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy {
    abstract fun <T> areCompatible(
        expectArg: Collection<T>,
        actualArg: Collection<T>,
        elementsEqual: (T, T) -> Boolean,
    ): Boolean

    data object Default : K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy() {
        override fun <T> areCompatible(
            expectArg: Collection<T>,
            actualArg: Collection<T>,
            elementsEqual: (T, T) -> Boolean,
        ): Boolean {
            return expectArg.size == actualArg.size && expectArg.zip(actualArg).all { (e1, e2) -> elementsEqual(e1, e2) }
        }
    }

    internal data object ExpectIsSubsetOfActual : K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy() {
        override fun <T> areCompatible(
            expectArg: Collection<T>,
            actualArg: Collection<T>,
            elementsEqual: (T, T) -> Boolean,
        ): Boolean {
            return expectArg.all { e1 ->
                actualArg.any { e2 -> elementsEqual(e1, e2) }
            }
        }
    }
}
