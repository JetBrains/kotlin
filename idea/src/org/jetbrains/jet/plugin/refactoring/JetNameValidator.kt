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

package org.jetbrains.jet.plugin.refactoring

import java.util.HashSet
import java.util.Collections

public abstract class JetNameValidator {
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

    protected abstract fun validateInner(name: String): Boolean
}

public object EmptyValidator : JetNameValidator() {
    override fun validateInner(name: String): Boolean = true
}

public open class CollectingValidator(
        existingNames: Collection<String> = Collections.emptySet(),
        val filter: (String) -> Boolean = { true }
): JetNameValidator() {
    private val suggestedSet = HashSet(existingNames)

    override fun validateInner(name: String): Boolean = name !in suggestedSet && filter(name)

    override fun validateName(name: String): String {
        val validatedName = super.validateName(name)
        suggestedSet.add(validatedName)
        return validatedName
    }
}

// TODO: To be used from Java
public class SimpleCollectingValidator : CollectingValidator()