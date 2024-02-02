/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.utils.DFS

abstract class BirElementClass<T : BirElement>(
    val javaClass: Class<T>,
    val id: Int,
    val hasImplementation: Boolean,
) : BirElementType<T>() {
    final override val possibleClasses: Set<BirElementClass<out T>>
        get() = setOf(this)

    val superClasses by lazy {
        (listOf(javaClass.superclass) + javaClass.interfaces)
            .mapNotNull { BirMetadata.allElementsByJavaClass[it] }
            .toSet()
    }

    private var _subClasses: Set<BirElementClass<*>>? = null
    val subClasses: Set<BirElementClass<out T>>
        get() {
            ensureAllSubclassesAreCalculated()

            // By definition, all subclasses are covariant
            @Suppress("UNCHECKED_CAST")
            return _subClasses!! as Set<BirElementClass<out T>>
        }

    val descendantClasses: Set<BirElementClass<out T>> by lazy {
        subClasses + subClasses.flatMap { it.descendantClasses }
    }

    val descendantClassesAndSelf: Set<BirElementClass<out T>>
        get() = descendantClasses + this

    override fun toString(): String = javaClass.name

    companion object {
        private var allSubclassesCalculated = false
        private fun ensureAllSubclassesAreCalculated() = synchronized(this) {
            if (!allSubclassesCalculated) {
                calculateAllSubclasses()
                allSubclassesCalculated = true
            }
        }

        private fun calculateAllSubclasses() {
            val allElements = BirMetadata.allElements
            val subClassesPerClass = allElements.associateWith { mutableSetOf<BirElementClass<*>>() }
            for (subClass in allElements) {
                for (superClass in subClass.superClasses) {
                    subClassesPerClass.getValue(superClass) += subClass
                }
            }
            for ((element, subClasses) in subClassesPerClass) {
                element._subClasses = subClasses
            }
        }
    }
}