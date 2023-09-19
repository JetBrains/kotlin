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

/**
 * A class name which is used to uniquely identify a Kotlin class.
 *
 * If local = true, the class represented by this id is either itself local or is an inner class of some local class. This also means that
 * the first non-class container of the class is not a package.
 * In the case of a local class, relativeClassName consists of a single name including all callables' and class' names all the way up to
 * the package, separated by dollar signs. If a class is an inner of local, relativeClassName would consist of two names,
 * the second one being the class' short name.
 */
class ClassId(val packageFqName: FqName, relativeClassName: FqName, local: Boolean) {
    val relativeClassName: FqName
    val isLocal: Boolean

    init {
        assert(!relativeClassName.isRoot) { "Class name must not be root: " + packageFqName + if (local) " (local)" else "" }
        this.relativeClassName = relativeClassName
        isLocal = local
    }

    constructor(packageFqName: FqName, topLevelName: Name) : this(packageFqName, FqName.topLevel(topLevelName), false)

    val parentClassId: ClassId?
        get() = if (!isNestedClass) null else ClassId(
            packageFqName,
            relativeClassName.parent(),
            isLocal
        )
    val shortClassName: Name
        get() = relativeClassName.shortName()

    fun createNestedClassId(name: Name): ClassId {
        return ClassId(packageFqName, relativeClassName.child(name), isLocal)
    }

    val outerClassId: ClassId?
        get() {
            val parent = relativeClassName.parent()
            return if (parent.isRoot) null else ClassId(packageFqName, parent, isLocal)
        }
    val outermostClassId: ClassId
        get() {
            var name = relativeClassName
            while (!name.parent().isRoot) name = name.parent()
            return ClassId(packageFqName, name, false)
        }
    val isNestedClass: Boolean
        get() = !relativeClassName.parent().isRoot

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
        return if (packageFqName.isRoot) relativeClassName.asString() else packageFqName.asString()
            .replace('.', '/') + "/" + relativeClassName.asString()
    }

    fun asFqNameString(): String {
        return if (packageFqName.isRoot) relativeClassName.asString() else packageFqName.asString() + "." + relativeClassName.asString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val id = o as ClassId
        return packageFqName == id.packageFqName && relativeClassName == id.relativeClassName && isLocal == id.isLocal
    }

    override fun hashCode(): Int {
        var result = packageFqName.hashCode()
        result = 31 * result + relativeClassName.hashCode()
        result = 31 * result + isLocal.hashCode()
        return result
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
         * @param string a string where packages are delimited by '/' and classes by '.', e.g. "kotlin/Map.Entry"
         */
        @JvmOverloads
        @JvmStatic
        fun fromString(string: String, isLocal: Boolean = false): ClassId {
            val lastSlashIndex = string.lastIndexOf("/")
            val packageName: String
            val className: String
            if (lastSlashIndex == -1) {
                packageName = ""
                className = string
            } else {
                packageName = string.substring(0, lastSlashIndex).replace('/', '.')
                className = string.substring(lastSlashIndex + 1)
            }
            return ClassId(FqName(packageName), FqName(className), isLocal)
        }
    }
}
