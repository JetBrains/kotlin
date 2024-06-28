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
package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * A class name which is used to uniquely identify a Kotlin class.
 *
 * If local = true, the class represented by this id is either itself local or is an inner class of some local class. This also means that
 * the first non-class container of the class is not a package.
 * In the case of a local class, relativeClassName consists of a single name including all callables' and class' names all the way up to
 * the package, separated by dollar signs. If a class is an inner of local, relativeClassName would consist of two names,
 * the second one being the class' short name.
 */
data class ClassId(val packageFqName: FqName, val relativeClassName: FqName, val isLocal: Boolean) {
    constructor(packageFqName: FqName, topLevelName: Name) : this(packageFqName, FqName.topLevel(topLevelName), isLocal = false)

    init {
        assert(!relativeClassName.isRoot) { "Class name must not be root: " + packageFqName + if (isLocal) " (local)" else "" }
    }

    val parentClassId: ClassId?
        get() = runIf(isNestedClass) {
            ClassId(packageFqName, relativeClassName.parent(), isLocal)
        }

    val shortClassName: Name
        get() = relativeClassName.shortName()

    val outerClassId: ClassId?
        get() {
            val parent = relativeClassName.parent()
            return runIf(!parent.isRoot) { ClassId(packageFqName, parent, isLocal) }
        }

    val outermostClassId: ClassId
        get() {
            var name = relativeClassName
            while (!name.parent().isRoot) {
                name = name.parent()
            }
            return ClassId(packageFqName, name, isLocal = false)
        }

    val isNestedClass: Boolean
        get() = !relativeClassName.parent().isRoot

    fun createNestedClassId(name: Name): ClassId {
        return ClassId(packageFqName, relativeClassName.child(name), isLocal)
    }

    fun asSingleFqName(): FqName {
        return if (packageFqName.isRoot) relativeClassName else FqName(packageFqName.asString() + "." + relativeClassName.asString())
    }

    fun startsWith(segment: Name): Boolean {
        return packageFqName.startsWith(segment)
    }

    /**
     * @return a string where packages are delimited by '/' and classes by '.', e.g. "kotlin/Map.Entry"
     */
    fun asString(): String {
        fun FqName.escapeSlashes(): String {
            val res = asString()
            if (res.contains('/')) {
                return "`$res`"
            }
            return res
        }

        return if (packageFqName.isRoot) {
            relativeClassName.escapeSlashes()
        } else {
            buildString {
                append(packageFqName.asString().replace('.', '/'))
                append("/")
                append(relativeClassName.escapeSlashes())
            }
        }
    }

    fun asFqNameString(): String {
        return if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString())
                append(".")
                append(relativeClassName.asString())
            }
        }
    }

    override fun toString(): String {
        return if (packageFqName.isRoot) "/" + asString() else asString()
    }

    companion object {
        @JvmStatic
        fun topLevel(topLevelFqName: FqName): ClassId {
            return ClassId(topLevelFqName.parent(), topLevelFqName.shortName())
        }

        /**
         * @param string a string where packages are delimited by '/' and classes by '.', e.g. "kotlin/Map.Entry".
         *               If class name contains slashes, it should be put into ticks, e.g. "package/`test/test`"
         */
        @JvmOverloads
        @JvmStatic
        fun fromString(string: String, isLocal: Boolean = false): ClassId {
            val tickIndex = string.indexOf('`')
            val lastSlashIndex = string.lastIndexOf("/", if (tickIndex == -1) string.length else tickIndex)
            val packageName: String
            val className: String
            if (lastSlashIndex == -1) {
                packageName = ""
                className = string.replace("`", "")
            } else {
                packageName = string.substring(0, lastSlashIndex).replace('/', '.')
                className = string.substring(lastSlashIndex + 1).replace("`", "")
            }
            return ClassId(FqName(packageName), FqName(className), isLocal)
        }
    }
}
