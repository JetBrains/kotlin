/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.builtins.arrays

import org.jetbrains.jet.generators.builtins.PrimitiveType
import org.jetbrains.jet.generators.builtins.generateBuiltIns.*
import java.io.PrintWriter

class GenerateArrays(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin"

    override fun generateBody() {
        for (kind in PrimitiveType.values()) {
            val s = kind.capitalized
            out.println("public class ${s}Array(public val size: Int) : Cloneable {")
            out.println("    public fun get(index: Int): $s")
            out.println("    public fun set(index: Int, value: $s): Unit")
            out.println()
            out.println("    public fun iterator(): ${s}Iterator")
            out.println()
            out.println("    public val indices: IntRange")
            out.println()
            out.println("    public override fun clone(): ${s}Array")
            out.println("}")
            out.println()
        }
    }
}
