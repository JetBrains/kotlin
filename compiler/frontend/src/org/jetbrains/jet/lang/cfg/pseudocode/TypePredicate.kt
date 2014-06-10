/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.util.SmartFMap
import org.jetbrains.jet.lang.types.TypeUtils

public trait TypePredicate: (JetType) -> Boolean {
    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
    override fun invoke(typeToCheck: JetType): Boolean
}

public data class SingleType(val targetType: JetType): TypePredicate {
    override fun invoke(typeToCheck: JetType): Boolean = JetTypeChecker.INSTANCE.equalTypes(typeToCheck, targetType)
    override fun toString(): String = targetType.render()
}

public object AllTypes : TypePredicate {
    override fun invoke(typeToCheck: JetType): Boolean = true

    override fun toString(): String = ""
}

private fun JetType.render(): String = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(this)

public fun <T> TypePredicate.expectedTypeFor(keys: Iterable<T>): Map<T, TypePredicate> =
        keys.fold(SmartFMap.emptyMap<T, TypePredicate>()) { (map, key) -> map.plus(key, this) }
