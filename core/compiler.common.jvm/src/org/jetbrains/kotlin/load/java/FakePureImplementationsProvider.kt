/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

object FakePureImplementationsProvider {
    fun getPurelyImplementedInterface(classFqName: ClassId): ClassId? = pureImplementationsClassIds[classFqName]
    fun getPurelyImplementedInterface(classFqName: FqName): FqName? = pureImplementationsFqNames[classFqName]

    private val pureImplementationsClassIds = mutableMapOf<ClassId, ClassId>()
    private infix fun ClassId.implementedWith(implementations: List<ClassId>) {
        implementations.associateWithTo(pureImplementationsClassIds) { this }
    }

    init {
        StandardClassIds.MutableList implementedWith fqNameListOf("java.util.ArrayList", "java.util.LinkedList")
        StandardClassIds.MutableSet implementedWith fqNameListOf("java.util.HashSet", "java.util.TreeSet", "java.util.LinkedHashSet")
        StandardClassIds.MutableMap implementedWith fqNameListOf(
            "java.util.HashMap", "java.util.TreeMap", "java.util.LinkedHashMap",
            "java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentSkipListMap"
        )
        ClassId.topLevel(FqName("java.util.function.Function")) implementedWith fqNameListOf("java.util.function.UnaryOperator")
        ClassId.topLevel(FqName("java.util.function.BiFunction")) implementedWith fqNameListOf("java.util.function.BinaryOperator")
    }

    private val pureImplementationsFqNames = pureImplementationsClassIds.map { (key, value) ->
        key.asSingleFqName() to value.asSingleFqName()
    }.toMap()

    private fun fqNameListOf(vararg names: String): List<ClassId> = names.map { ClassId.topLevel(FqName(it)) }
}
