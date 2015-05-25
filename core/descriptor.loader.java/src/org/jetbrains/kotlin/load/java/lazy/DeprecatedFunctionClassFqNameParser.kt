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

package org.jetbrains.kotlin.load.java.lazy

public object DeprecatedFunctionClassFqNameParser {
    /**
     * Checks that the given FQ name denotes some deprecated function class and returns the old and the new FQ name of the class
     * in case it does, or null otherwise, e.g.:
     * "kotlin.Function1" -> ("kotlin.Function1", "kotlin.jvm.functions.Function1")
     * "kotlin.ExtensionFunction1" -> ("kotlin.ExtensionFunction1", "kotlin.jvm.functions.Function2")
     * "kotlin.jvm.functions.Function1" -> null
     */
    public fun extractOldAndNewFqName(fqName: String): Pair<String, String>? {
        val arity = fqName.removePrefix("kotlin.Function")
        if (arity != fqName && arity.isInt()) {
            return Pair(fqName, "kotlin.jvm.functions.Function$arity")
        }

        val extensionArity = fqName.removePrefix("kotlin.ExtensionFunction")
        if (extensionArity != fqName && extensionArity.isInt()) {
            return Pair(fqName, "kotlin.jvm.functions.Function${extensionArity.toInt() + 1}")
        }

        return null
    }

    public fun isDeprecatedFunctionClassFqName(fqName: String): Boolean {
        return extractOldAndNewFqName(fqName) != null
    }

    private fun String.isInt() =
            try {
                toInt()
                true
            }
            catch (e: NumberFormatException) {
                false
            }
}
