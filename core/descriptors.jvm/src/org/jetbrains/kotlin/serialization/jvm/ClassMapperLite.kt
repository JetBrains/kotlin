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

package org.jetbrains.kotlin.serialization.jvm

// The purpose of this class is to map Kotlin classes to JVM bytecode desc strings, as KotlinTypeMapper does in the backend.
// It's used as an optimization during serialization/deserialization: if there's no JVM signature for a method/property/constructor,
// it means that the JVM signature should be trivially computable from the Kotlin signature with this class.
// It's not required to support everything in KotlinTypeMapper, but the more it does, the more we save on JVM signatures in proto metadata.
//
// WARNING: improving the behavior of this class MAY BREAK BINARY COMPATIBILITY of code compiled by Kotlin, because it may make
// the new compiler skip writing the signatures it now thinks are trivial, and the old compiler would recreate them incorrectly.
object ClassMapperLite {
    // Kotlin ClassId -> JVM desc
    // e.g. "kotlin.IntArray" -> "[I"
    //      "kotlin.String.Companion" -> "Lkotlin/jvm/internal/StringCompanionObject"
    //      "kotlin/collections/Map.Entry" -> "Ljava/util/Map$Entry"
    private val map: Map<String, String> = mutableMapOf<String, String>().apply {
        val primitives = listOf(
            "Boolean", "Z",
            "Char", "C",
            "Byte", "B",
            "Short", "S",
            "Int", "I",
            "Float", "F",
            "Long", "J",
            "Double", "D"
        )

        for (i in primitives.indices step 2) {
            put("kotlin/${primitives[i]}", primitives[i + 1])
            put("kotlin/${primitives[i]}Array", "[${primitives[i + 1]}")
        }

        put("kotlin/Unit", "V")

        fun add(kotlinSimpleName: String, javaInternalName: String) {
            put("kotlin/$kotlinSimpleName", "L$javaInternalName;")
        }

        add("Any", "java/lang/Object")
        add("Nothing", "java/lang/Void")
        add("Annotation", "java/lang/annotation/Annotation")

        for (klass in listOf("String", "CharSequence", "Throwable", "Cloneable", "Number", "Comparable", "Enum")) {
            add(klass, "java/lang/$klass")
        }

        for (klass in listOf("Iterator", "Collection", "List", "Set", "Map", "ListIterator")) {
            add("collections/$klass", "java/util/$klass")
            add("collections/Mutable$klass", "java/util/$klass")
        }

        add("collections/Iterable", "java/lang/Iterable")
        add("collections/MutableIterable", "java/lang/Iterable")
        add("collections/Map.Entry", "java/util/Map\$Entry")
        add("collections/MutableMap.MutableEntry", "java/util/Map\$Entry")

        for (i in 0..22) {
            add("Function$i", "kotlin/jvm/functions/Function$i")
            add("reflect/KFunction$i", "kotlin/reflect/KFunction")
        }

        for (klass in listOf("Char", "Byte", "Short", "Int", "Float", "Long", "Double", "String", "Enum")) {
            add("$klass.Companion", "kotlin/jvm/internal/${klass}CompanionObject")
        }
    }

    /**
     * @param classId the name of the class in the format: "org/foo/bar/Test.Inner"
     */
    @JvmStatic
    fun mapClass(classId: String): String {
        return map[classId] ?: "L${classId.replace('.', '$')};"
    }
}
