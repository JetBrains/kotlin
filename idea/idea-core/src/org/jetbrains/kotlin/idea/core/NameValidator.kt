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

package org.jetbrains.kotlin.idea.core

import java.util.HashSet
import java.util.Collections

public abstract class NameValidator {
    /**
     * Validates name, and slightly improves it by adding number to name in case of conflicts
     * @param name to check it in scope
     * @return name or nameI, where I is number
     */
    public open fun validateName(name: String): String {
        if (validateInner(name)) return name
        var i = 1
        while (!validateInner(name + i)) {
            ++i
        }

        return name + i
    }

    /**
     * Validates name using set of variants which are tried in succession (and extended with suffixes if necessary)
     * For example, when given sequence of a, b, c possible names are tried out in the following order: a, b, c, a1, b1, c1, a2, b2, c2, ...
     * @param names to check it in scope
     * @return name or nameI, where name is one of variants and I is a number
     */
    public fun validateNameWithVariants(vararg names: String): String {
        var i = 0
        while (true) {
            for (name in names) {
                val candidate = if (i > 0) name + i else name
                if (validateInner(candidate)) return candidate
            }
            i++
        }
    }

    protected abstract fun validateInner(name: String): Boolean
}

public object EmptyValidator : NameValidator() {
    override fun validateInner(name: String): Boolean = true
}

public open class CollectingValidator(
        existingNames: Collection<String> = Collections.emptySet(),
        val filter: (String) -> Boolean = { true }
): NameValidator() {
    private val suggestedSet = HashSet(existingNames)

    override fun validateInner(name: String): Boolean {
        if (name !in suggestedSet && filter(name)) {
            suggestedSet.add(name)
            return true
        }
        return false
    }
}

// TODO: To be used from Java
public class SimpleCollectingValidator : CollectingValidator()
