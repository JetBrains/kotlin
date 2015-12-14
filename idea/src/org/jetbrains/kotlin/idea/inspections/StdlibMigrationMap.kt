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

package org.jetbrains.kotlin.idea.inspections

data class MethodFQName(val className: String, val methodName: String)

object StdlibMigrationMap {
    val methodMap = hashMapOf(
            MethodFQName("kotlin.jvm.ClassMapping", "getKotlin") to MethodFQName("kotlin.jvm.JvmClassMappingKt", "getKotlinClass"),
            MethodFQName("kotlin.jvm.ClassMapping", "getJava") to MethodFQName("kotlin.jvm.JvmClassMappingKt", "getJavaClass")
    )

    val classMap = hashMapOf(
            "kotlin.ArraysKt"      to "kotlin.collections.ArraysKt",
            "kotlin.CharsKt"       to "kotlin.text.CharsKt",
            "kotlin.CollectionsKt" to "kotlin.collections.CollectionsKt",
            "kotlin.MapsKt"        to "kotlin.collections.MapsKt",
            "kotlin.RangesKt"      to "kotlin.ranges.RangesKt",
            "kotlin.SequencesKt"   to "kotlin.sequences.SequencesKt",
            "kotlin.SetsKt"        to "kotlin.collections.SetsKt",
            "kotlin.StringsKt"     to "kotlin.text.StringsKt",
            "kotlin.support.AbstractIterator" to "kotlin.collections.AbstractIterator"
    )
}
