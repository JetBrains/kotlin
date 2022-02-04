/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.jvm.serialization

import org.jetbrains.kotlin.metadata.serialization.StringTable

open class SimpleStringTable : StringTable {
    val strings = ArrayList<String>()
    private val map = HashMap<String, Int>()
    private val localNames = LinkedHashSet<Int>()

    override fun getStringIndex(string: String): Int =
        map.getOrPut(string) {
            strings.size.apply {
                strings.add(string)
            }
        }

    // We use the following format to encode ClassId: "pkg/Outer.Inner".
    // It represents a unique name, but such names don't usually appear in the constant pool, so we're writing "Lpkg/Outer$Inner;"
    // instead and an instruction to drop the first and the last character in this string and replace all '$' with '.'.
    // This works most of the time, except in two rare cases:
    // - the name of the class or any of its outer classes contains dollars. In this case we're just storing the described
    //   string literally: "pkg/Outer.Inner$with$dollars"
    // - the class is local or nested in local. In this case we're also storing the literal string, and also storing the fact that
    //   this name represents a local class in a separate list
    override fun getQualifiedClassNameIndex(className: String, isLocal: Boolean): Int {
        map[className]?.let { recordedIndex ->
            // If we already recorded such string, we only return its index if it's local and our name is local
            // OR it's not local and our name is not local as well
            if (isLocal == (recordedIndex in localNames)) {
                return recordedIndex
            }
        }

        val index = strings.size
        if (isLocal) {
            localNames.add(index)
        }

        // If the class is local or any of its outer class names contains '$', store a literal string
        if (isLocal || '$' in className) {
            strings.add(className)
        } else {
            strings.add("L${className.replace('.', '$')};")
        }

        map[className] = index

        return index
    }
}
