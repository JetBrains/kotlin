/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.name.FqName

object FakePureImplementationsProvider {
    fun getPurelyImplementedInterface(classFqName: FqName): FqName? = pureImplementations[classFqName]

    private val pureImplementations = hashMapOf<FqName, FqName>()
    private infix fun FqName.implementedWith(implementations: List<FqName>) {
        implementations.associateWithTo(pureImplementations) { this }
    }

    init {
        FQ_NAMES.mutableList implementedWith fqNameListOf("java.util.ArrayList", "java.util.LinkedList")
        FQ_NAMES.mutableSet implementedWith fqNameListOf("java.util.HashSet", "java.util.TreeSet", "java.util.LinkedHashSet")
        FQ_NAMES.mutableMap implementedWith fqNameListOf(
            "java.util.HashMap", "java.util.TreeMap", "java.util.LinkedHashMap",
            "java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentSkipListMap"
        )
        FqName("java.util.function.Function") implementedWith fqNameListOf("java.util.function.UnaryOperator")
        FqName("java.util.function.BiFunction") implementedWith fqNameListOf("java.util.function.BinaryOperator")
    }

    private fun fqNameListOf(vararg names: String): List<FqName> = names.map(::FqName)
}
