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

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties.getPropertyNameCandidatesBySpecialGetterName


fun propertyNameByGetMethodName(methodName: Name): Name?
        = propertyNameFromAccessorMethodName(methodName, "get") ?: propertyNameFromAccessorMethodName(methodName, "is", removePrefix = false)

fun propertyNameBySetMethodName(methodName: Name, withIsPrefix: Boolean): Name?
        = propertyNameFromAccessorMethodName(methodName, "set", addPrefix = if (withIsPrefix) "is" else null)

fun propertyNamesBySetMethodName(methodName: Name)
        = listOf(propertyNameBySetMethodName(methodName, false), propertyNameBySetMethodName(methodName, true)).filterNotNull()

private fun propertyNameFromAccessorMethodName(methodName: Name, prefix: String, removePrefix: Boolean = true, addPrefix: String? = null): Name? {
    if (methodName.isSpecial) return null
    val identifier = methodName.identifier
    if (!identifier.startsWith(prefix)) return null
    if (identifier.length == prefix.length) return null
    if (identifier[prefix.length] in 'a'..'z') return null

    if (addPrefix != null) {
        assert(removePrefix)
        return Name.identifier(addPrefix + identifier.removePrefix(prefix))
    }

    if (!removePrefix) return methodName
    val name = identifier.removePrefix(prefix).decapitalizeSmart(asciiOnly = true)
    if (!Name.isValidIdentifier(name)) return null
    return Name.identifier(name)
}

fun getPropertyNamesCandidatesByAccessorName(name: Name): List<Name> {
    val nameAsString = name.asString()

    if (JvmAbi.isGetterName(nameAsString)) {
        return listOfNotNull(propertyNameByGetMethodName(name))
    }

    if (JvmAbi.isSetterName(nameAsString)) {
        return propertyNamesBySetMethodName(name)
    }

    return getPropertyNameCandidatesBySpecialGetterName(name)
}