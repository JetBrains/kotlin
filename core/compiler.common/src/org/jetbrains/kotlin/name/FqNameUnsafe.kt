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

import java.util.regex.Pattern

/**
 * Like [FqName] but allows '<' and '>' characters in name.
 */
class FqNameUnsafe(
    private val nameSegments: Array<Name>,
    // cache
    @Transient
    private var safe: FqName? = null
) {
    private val parent: FqNameUnsafe
        get() =
            when (nameSegments.size) {
                0 -> error("root")
                1 -> FqName.ROOT.toUnsafe()
                else -> FqNameUnsafe(nameSegments.dropLast(1).toTypedArray())
            }

    private val shortName: Name?
        get() = nameSegments.lastOrNull()


    constructor(fqName: String, safe: FqName?) : this(computeNamesFromString(fqName), safe)
    constructor(fqName: String) : this(computeNamesFromString(fqName), null)

    fun asString(): String = nameSegments.joinToString(".")

    val isSafe: Boolean
        get() = safe != null || nameSegments.none { it.isSpecial }

    fun toSafe(): FqName {
        if (safe != null) {
            return safe!!
        }
        safe = FqName(this)
        return safe!!
    }

    val isRoot: Boolean
        get() = nameSegments.isEmpty()

    fun parent(): FqNameUnsafe = parent

    fun child(name: Name): FqNameUnsafe = FqNameUnsafe(nameSegments + name)

    fun shortName(): Name = shortName!!

    fun shortNameOrSpecial(): Name {
        return if (isRoot) {
            ROOT_NAME
        } else {
            shortName()
        }
    }

    fun pathSegments(): List<Name> = nameSegments.asList()

    fun startsWith(segment: Name): Boolean =
        when (nameSegments.size) {
            0 -> false
            else -> segment == nameSegments[0]
        }

    override fun toString(): String = if (isRoot) ROOT_NAME.asString() else asString()

    override fun equals(o: Any?): Boolean = when {
        this === o -> true
        o !is FqNameUnsafe -> false
        else -> nameSegments contentEquals o.nameSegments
    }

    override fun hashCode(): Int = nameSegments.fold(0) { a, s -> a + s.hashCode() }

    companion object {
        private val ROOT_NAME = Name.special("<root>")
        private val SPLIT_BY_DOTS = Pattern.compile("\\.")

        fun isValid(qualifiedName: String?): Boolean {
            // TODO: There's a valid name with escape char ``
            return qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0
        }

        @JvmStatic
        fun topLevel(shortName: Name): FqNameUnsafe = FqNameUnsafe(arrayOf(shortName))

        @JvmStatic
        private fun computeNamesFromString(fqNameString: String): Array<Name> {
            return if (fqNameString.isBlank()) emptyArray()
            else SPLIT_BY_DOTS.split(fqNameString).map {
                Name.guessByFirstCharacter(it)
            }.toTypedArray()
        }
    }
}


