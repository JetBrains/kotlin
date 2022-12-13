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

class FqName {
    private val fqName: FqNameUnsafe

    constructor(fqName: String) {
        this.fqName = FqNameUnsafe(fqName, this)
    }

    constructor(nameSegments: Array<Name>) {
        this.fqName = FqNameUnsafe(nameSegments, this)
    }

    constructor(fqName: FqNameUnsafe) {
        this.fqName = fqName
    }

    fun asString(): String {
        return fqName.asString()
    }

    fun toUnsafe(): FqNameUnsafe {
        return fqName
    }

    val isRoot: Boolean
        get() = fqName.isRoot

    fun parent(): FqName = fqName.parent().toSafe()

    fun child(name: Name): FqName {
        return FqName(fqName.child(name, this))
    }

    fun shortName(): Name {
        return fqName.shortName()
    }

    fun shortNameOrSpecial(): Name {
        return fqName.shortNameOrSpecial()
    }

    fun pathSegments(): List<Name> {
        return fqName.pathSegments()
    }

    fun startsWith(segment: Name): Boolean {
        return fqName.startsWith(segment)
    }

    override fun toString(): String {
        return fqName.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FqName) return false
        return fqName == other.fqName
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromSegments(names: List<String>): FqName {
            return FqName(names.map { Name.guessByFirstCharacter(it) }.toTypedArray())
        }

        @JvmStatic
        @JvmName("fromSegmentsNames")
        fun fromSegments(names: List<Name>): FqName {
            return FqName(names.toTypedArray())
        }

        @JvmField
        val ROOT = FqName("")
        @JvmStatic
        fun topLevel(shortName: Name): FqName {
            return FqName(FqNameUnsafe.topLevel(shortName))
        }
    }
}