/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties.getPropertyNameCandidatesBySpecialGetterName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeFirstWord
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmartForCompiler
import java.util.ArrayList

fun propertyNameByGetMethodName(methodName: Name): Name? =
    propertyNameFromAccessorMethodName(methodName, "get") ?: propertyNameFromAccessorMethodName(methodName, "is", removePrefix = false)

fun propertyNameBySetMethodName(methodName: Name, withIsPrefix: Boolean): Name? =
    propertyNameFromAccessorMethodName(methodName, "set", addPrefix = if (withIsPrefix) "is" else null)

fun propertyNamesBySetMethodName(methodName: Name): List<Name> =
    listOfNotNull(propertyNameBySetMethodName(methodName, false), propertyNameBySetMethodName(methodName, true))

fun propertyNamesByAccessorName(name: Name): List<Name> = listOfNotNull(
    propertyNameByGetMethodName(name),
    propertyNameBySetMethodName(name, withIsPrefix = true),
    propertyNameBySetMethodName(name, withIsPrefix = false)
)

private fun propertyNameFromAccessorMethodName(
    methodName: Name,
    prefix: String,
    removePrefix: Boolean = true,
    addPrefix: String? = null
): Name? {
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
    val name = identifier.removePrefix(prefix).decapitalizeSmartForCompiler(asciiOnly = true)
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

fun possibleGetMethodNames(propertyName: Name): List<Name> {
    val result = ArrayList<Name>(3)
    val identifier = propertyName.identifier

    if (JvmAbi.startsWithIsPrefix(identifier)) {
        result.add(propertyName)
    }

    val capitalize1 = identifier.capitalizeAsciiOnly()
    val capitalize2 = identifier.capitalizeFirstWord(asciiOnly = true)
    result.add(Name.identifier("get$capitalize1"))
    if (capitalize2 != capitalize1) {
        result.add(Name.identifier("get$capitalize2"))
    }

    return result
        .filter { propertyNameByGetMethodName(it) == propertyName } // don't accept "uRL" for "getURL" etc
}

fun setMethodName(getMethodName: Name): Name {
    val identifier = getMethodName.identifier
    val prefix = when {
        identifier.startsWith("get") -> "get"
        identifier.startsWith("is") -> "is"
        else -> throw IllegalArgumentException()
    }
    return Name.identifier("set" + identifier.removePrefix(prefix))
}
