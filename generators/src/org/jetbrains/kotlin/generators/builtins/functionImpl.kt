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

package org.jetbrains.kotlin.generators.builtins.functionImpl

import org.jetbrains.kotlin.generators.builtins.functions.MAX_PARAM_COUNT
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator.Language
import java.io.PrintWriter

class GenerateFunctionImpl(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin.jvm.internal"

    override val language = Language.JAVA

    override fun generateBody() {
        val n = MAX_PARAM_COUNT

        out.print("""import kotlin.Function;
import kotlin.jvm.functions.*;

import java.io.Serializable;

public abstract class FunctionImpl
        implements Function, Serializable,""")

        for (i in 0..n) {
            if (i % 10 == 0) {
                // Insert newline sometimes to avoid very long lines
                out.println()
                out.print("                   ")
            }
            out.print("Function$i")
            if (i < n) out.print(", ")
        }

        out.println(" {")

        out.println("""
    public abstract int getArity();

    public Object invokeVararg(Object... p) {
        throw new UnsupportedOperationException();
    }

    private void checkArity(int expected) {
        if (getArity() != expected) {
            throwWrongArity(expected);
        }
    }

    private void throwWrongArity(int expected) {
        throw new IllegalStateException("Wrong function arity, expected: " + expected + ", actual: " + getArity());
    }""")

        for (i in 0..n) {
            out.println()
            out.println("    @Override")
            out.println("    public Object invoke(" + (1..i).joinToString { "Object p$it" } + ") {")
            out.println("        checkArity($i);")
            out.println("        return invokeVararg(" + (1..i).joinToString { "p$it" } + ");")
            out.println("    }")
        }

        out.println("}")
    }
}
