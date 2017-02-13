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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.*

class ImportPath {
    private val fqName: FqName
    val alias: Name?
    val isAllUnder: Boolean

    @JvmOverloads constructor(fqName: FqName, isAllUnder: Boolean, alias: Name? = null) {
        this.fqName = fqName
        this.isAllUnder = isAllUnder
        this.alias = alias
    }

    constructor(pathStr: String) {
        if (pathStr.endsWith(".*")) {
            this.isAllUnder = true
            this.fqName = FqName(pathStr.substring(0, pathStr.length - 2))
        }
        else {
            this.isAllUnder = false
            this.fqName = FqName(pathStr)
        }

        alias = null
    }

    val pathStr: String
        get() = fqName.toUnsafe().render() + if (isAllUnder) ".*" else ""

    override fun toString(): String {
        return pathStr + if (alias != null) " as " + alias.asString() else ""
    }

    fun fqnPart(): FqName {
        return fqName
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

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val path = o as ImportPath?

        if (isAllUnder != path!!.isAllUnder) return false
        if (if (alias != null) alias != path.alias else path.alias != null) return false
        if (fqName != path.fqName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        result = 31 * result + (alias?.hashCode() ?: 0)
        result = 31 * result + if (isAllUnder) 1 else 0
        return result
    }
}
