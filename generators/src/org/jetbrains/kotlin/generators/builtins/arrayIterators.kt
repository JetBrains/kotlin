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

package org.jetbrains.kotlin.generators.builtins.arrayIterators

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.*
import java.io.PrintWriter

class GenerateArrayIterators(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin.jvm.internal"

    override fun generateBody() {
        for (kind in PrimitiveType.values) {
            val s = kind.capitalized
            out.println("private class Array${s}Iterator(private val array: ${s}Array) : ${s}Iterator() {")
            out.println("    private var index = 0")
            out.println("    override fun hasNext() = index < array.size")
            out.println("    override fun next$s() = array[index++]")
            out.println("}")
            out.println()
        }
        for (kind in PrimitiveType.values) {
            val s = kind.capitalized
            out.println("public fun iterator(array: ${s}Array): ${s}Iterator = Array${s}Iterator(array)")
        }
    }
}
