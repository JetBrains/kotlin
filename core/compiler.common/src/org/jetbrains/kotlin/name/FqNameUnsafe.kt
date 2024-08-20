/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.name

import java.util.regex.Pattern

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
        val lastDot = indexOfLastDotWithBackticksSupport(fqName)
        if (lastDot >= 0) {
            shortName = Name.guessByFirstCharacter(fqName.substring(lastDot + 1))
            parent = FqNameUnsafe(fqName.substring(0, lastDot))
        } else {
            shortName = Name.guessByFirstCharacter(fqName)
            parent = FqName.ROOT.toUnsafe()
        }
    }

    private fun indexOfLastDotWithBackticksSupport(fqName: String): Int {
        var index = fqName.length - 1
        var isBacktick = false

        while (index >= 0) {
            when (fqName[index]) {
                '.' if !isBacktick -> return index
                '`' -> isBacktick = !isBacktick
                '\\' -> index--
            }

            index--
        }

        return -1
    }

    fun asString(): String {
        return fqName
    }

    val isSafe: Boolean get() = safe != null || asString().indexOf('<') < 0

    fun toSafe(): FqName =
        safe ?: FqName(this).also { safe = it }

    val isRoot: Boolean
        get() = fqName.isEmpty()

    fun parent(): FqNameUnsafe {
        parent?.let {
            return it
        }

        check(!isRoot) { "root" }

        compute()

        return parent!!
    }

    fun child(name: Name): FqNameUnsafe {
        val childFqName = if (isRoot) {
            name.asString()
        } else {
            fqName + "." + name.asString()
        }
        return FqNameUnsafe(childFqName, this, name)
    }

    fun shortName(): Name {
        shortName?.let {
            return it
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
        fun collectSegmentsOf(fqName: FqNameUnsafe): MutableList<Name> {
            if (fqName.isRoot) {
                return ArrayList()
            }

            val parentSegments = collectSegmentsOf(fqName.parent())
            parentSegments.add(fqName.shortName())
            return parentSegments
        }

        return collectSegmentsOf(this)
    }

    fun startsWith(segment: Name): Boolean {
        if (isRoot) return false

        val firstDot = fqName.indexOf('.')
        val fqNameFirstSegmentLength = if (firstDot == -1) fqName.length else firstDot
        val segmentAsString = segment.asString()
        return fqNameFirstSegmentLength == segmentAsString.length &&
                fqName.regionMatches(0, segmentAsString, 0, fqNameFirstSegmentLength)
    }

    fun startsWith(other: FqNameUnsafe): Boolean {
        if (isRoot) return false

        val thisLength = fqName.length
        val otherLength = other.fqName.length
        if (thisLength < otherLength) return false

        return (thisLength == otherLength || fqName[otherLength] == '.') &&
                fqName.regionMatches(0, other.fqName, 0, otherLength)
    }

    override fun toString(): String {
        return if (isRoot) ROOT_NAME.asString() else fqName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is FqNameUnsafe) return false

        if (fqName != o.fqName) return false

        return true
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }

    companion object {
        private val ROOT_NAME = Name.special("<root>")
        private val SPLIT_BY_DOTS: Pattern = Pattern.compile("\\.")

        @JvmStatic
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
