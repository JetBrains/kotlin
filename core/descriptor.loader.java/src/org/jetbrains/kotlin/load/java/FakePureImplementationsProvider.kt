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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils

public object FakePureImplementationsProvider {
    public fun getPurelyImplementedInterface(classFqName: FqName): FqName? = when(classFqName) {
        in MUTABLE_LISTS_IMPLEMENTATIONS -> MUTABLE_LIST_FQ_NAME
        in MUTABLE_MAPS_IMPLEMENTATIONS -> MUTABLE_MAP_FQ_NAME
        in MUTABLE_SETS_IMPLEMENTATIONS -> MUTABLE_SET_FQ_NAME
        else -> null
    }

    private val MUTABLE_LIST_FQ_NAME = KotlinBuiltIns.FQ_NAMES.mutableList
    private val MUTABLE_SET_FQ_NAME = KotlinBuiltIns.FQ_NAMES.mutableSet
    private val MUTABLE_MAP_FQ_NAME = KotlinBuiltIns.FQ_NAMES.mutableMap

    private val MUTABLE_LISTS_IMPLEMENTATIONS = setOfFqNames("java.util.ArrayList", "java.util.LinkedList")
    private val MUTABLE_MAPS_IMPLEMENTATIONS = setOfFqNames(
            "java.util.HashMap", "java.util.TreeMap", "java.util.LinkedHashMap",
            "java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentSkipListMap"
    )
    private val MUTABLE_SETS_IMPLEMENTATIONS = setOfFqNames(
            "java.util.HashSet", "java.util.TreeSet", "java.util.LinkedHashSet"
    )
}

private fun setOfFqNames(vararg names: String) = names.map { FqName(it) }.toSet()
