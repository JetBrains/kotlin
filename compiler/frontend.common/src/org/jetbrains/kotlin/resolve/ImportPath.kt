/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.*

data class ImportPath @JvmOverloads constructor(val fqName: FqName, val isAllUnder: Boolean, val alias: Name? = null) {

    val pathStr: String
        get() = fqName.toUnsafe().render() + if (isAllUnder) ".*" else ""

    override fun toString(): String {
        return pathStr + if (alias != null) " as " + alias.asString() else ""
    }

    fun hasAlias(): Boolean {
        return alias != null
    }

    val importedName: Name?
        get() {
            if (!isAllUnder) {
                return alias ?: fqName.shortName()
            }

            return null
        }

    companion object {
        @JvmStatic
        fun fromString(pathStr: String): ImportPath {
            return if (pathStr.endsWith(".*")) {
                ImportPath(FqName(pathStr.substring(0, pathStr.length - 2)), isAllUnder = true)
            } else {
                ImportPath(FqName(pathStr), isAllUnder = false)

            }
        }
    }
}
