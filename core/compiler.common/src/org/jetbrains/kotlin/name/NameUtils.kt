/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.name

object NameUtils {
    private val SANITIZE_AS_JAVA_INVALID_CHARACTERS = "[^\\p{L}\\p{Digit}]".toRegex()

    @JvmStatic
    val CONTEXT_RECEIVER_PREFIX = "\$context_receiver"

    @JvmStatic
    fun sanitizeAsJavaIdentifier(name: String): String {
        return SANITIZE_AS_JAVA_INVALID_CHARACTERS.replace(name, "_")
    }

    /**
     * Capitalizes the short name of the file (without extension) and sanitizes it so that it's a valid Java identifier.
     * E.g. "fileName" -> "FileName", "1" -> "_1", "" -> "_"
     */
    @JvmStatic
    fun getPackagePartClassNamePrefix(shortFileName: String): String =
        if (shortFileName.isEmpty())
            "_"
        else
            capitalizeAsJavaClassName(sanitizeAsJavaIdentifier(shortFileName))

    @JvmStatic
    private fun capitalizeAsJavaClassName(str: String): String =
        // NB `uppercase` uses Locale.ROOT and is locale-independent.
        // See Javadoc on java.lang.String.toUpperCase() for more details.
        if (Character.isJavaIdentifierStart(str[0]))
            str[0].uppercase() + str.substring(1)
        else
            "_$str"

    // "pkg/someScript.kts" -> "SomeScript"
    @JvmStatic
    fun getScriptNameForFile(filePath: String): Name =
        Name.identifier(NameUtils.getPackagePartClassNamePrefix(filePath.substringAfterLast('/').substringBeforeLast('.')))

    @JvmStatic
    fun hasName(name: Name) = name != SpecialNames.NO_NAME_PROVIDED && name != SpecialNames.ANONYMOUS

    @JvmStatic
    fun delegateFieldName(index: Int): Name {
        return Name.identifier("\$\$delegate_$index")
    }

    @JvmStatic
    fun propertyDelegateName(propertyName: Name): Name {
        return Name.identifier("${propertyName.asString()}\$delegate")
    }

    @JvmStatic
    fun contextReceiverName(index: Int): Name =
        Name.identifier("${CONTEXT_RECEIVER_PREFIX}_$index")
}
