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
import kotlin.jvm.functions.Function1

/**
 * Like [FqName] but allows '<' and '>' characters in name.
 */
class FqNameUnsafe {
    private val fqName: String

    // cache
    @Transient
    private var safe: FqName? = null

    @Transient
    private var parent: FqNameUnsafe? = null

    @Transient
    private var shortName: Name? = null

    internal constructor(fqName: String, safe: FqName) {
        this.fqName = fqName
        this.safe = safe
    }

    constructor(fqName: String) {
        this.fqName = fqName
    }

    private constructor(fqName: String, parent: FqNameUnsafe, shortName: Name) {
        this.fqName = fqName
        this.parent = parent
        this.shortName = shortName
    }

    private fun compute() {
        val lastDot = fqName.lastIndexOf('.')
        if (lastDot >= 0) {
            shortName = Name.guessByFirstCharacter(fqName.substring(lastDot + 1))
            parent = FqNameUnsafe(fqName.substring(0, lastDot))
        } else {
            shortName = Name.guessByFirstCharacter(fqName)
            parent = FqName.ROOT.toUnsafe()
        }
    }

    fun asString(): String {
        return fqName
    }

    val isSafe: Boolean
        get() = safe != null || asString().indexOf('<') < 0

    fun toSafe(): FqName {
        if (safe != null) {
            return safe!!
        }
        safe = FqName(this)
        return safe!!
    }

    val isRoot: Boolean
        get() = fqName.isEmpty()

    fun parent(): FqNameUnsafe {
        if (parent != null) {
            return parent!!
        }
        check(!isRoot) { "root" }
        compute()
        return parent!!
    }

    fun child(name: Name): FqNameUnsafe {
        val childFqName: String
        childFqName = if (isRoot) {
            name.asString()
        } else {
            fqName + "." + name.asString()
        }
        return FqNameUnsafe(childFqName, this, name)
    }

    fun shortName(): Name {
        if (shortName != null) {
            return shortName!!
        }
        check(!isRoot) { "root" }
        compute()
        return shortName!!
    }

    fun shortNameOrSpecial(): Name {
        return if (isRoot) {
            ROOT_NAME
        } else {
            shortName()
        }
    }

    fun pathSegments(): List<Name> {
        return if (isRoot) emptyList()
        else SPLIT_BY_DOTS.split(fqName).map {
            Name.guessByFirstCharacter(it)
        }
    }

    fun startsWith(segment: Name): Boolean {
        if (isRoot) return false
        val firstDot = fqName.indexOf('.')
        val segmentAsString = segment.asString()
        return fqName.regionMatches(
            0,
            segmentAsString,
            0,
            if (firstDot == -1) Math.max(fqName.length, segmentAsString.length) else firstDot
        )
    }

    override fun toString(): String {
        return if (isRoot) ROOT_NAME.asString() else fqName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is FqNameUnsafe) return false
        return if (fqName != o.fqName) false else true
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }

    companion object {
        private val ROOT_NAME = Name.special("<root>")
        private val SPLIT_BY_DOTS = Pattern.compile("\\.")

        fun isValid(qualifiedName: String?): Boolean {
            // TODO: There's a valid name with escape char ``
            return qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0
        }

        @JvmStatic
        fun topLevel(shortName: Name): FqNameUnsafe {
            return FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName)
        }
    }
}