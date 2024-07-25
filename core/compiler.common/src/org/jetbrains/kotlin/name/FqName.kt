/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.name

class FqName {
    private val fqName: FqNameUnsafe

    // cache
    @Transient
    private var parent: FqName? = null

    constructor(fqName: String) {
        this.fqName = FqNameUnsafe(fqName, this)
    }

    constructor(fqName: FqNameUnsafe) {
        this.fqName = fqName
    }

    private constructor(fqName: FqNameUnsafe, parent: FqName) {
        this.fqName = fqName
        this.parent = parent
    }

    fun asString(): String {
        return fqName.asString()
    }

    fun toUnsafe(): FqNameUnsafe {
        return fqName
    }

    val isRoot: Boolean
        get() = fqName.isRoot

    fun parent(): FqName {
        parent?.let {
            return it
        }

        check(!isRoot) { "root" }

        return FqName(fqName.parent()).also {
            parent = it
        }
    }

    fun child(name: Name): FqName {
        return FqName(fqName.child(name), this)
    }

    fun shortName(): Name {
        return fqName.shortName()
    }

    fun shortNameOrSpecial(): Name {
        return fqName.shortNameOrSpecial()
    }

    /**
     * Consider using [properPathSegments].
     */
    fun pathSegments(): List<Name> {
        return fqName.pathSegments()
    }

    /**
     * Returns path segments (`[a,b,c]` for `a.b.c`), but unlike [pathSegments],
     * gathers information from [parent] and [shortName].
     * This allows handling fqName parts containing dots as part of their name.
     *
     * The original function is left intact to avoid introducing possible unexpected behavior changes in K1.
     */
    fun properPathSegments(): List<Name> {
        return fqName.properPathSegments()
    }

    fun startsWith(segment: Name): Boolean {
        return fqName.startsWith(segment)
    }

    fun startsWith(other: FqName): Boolean {
        return fqName.startsWith(other.fqName)
    }

    override fun toString(): String {
        return fqName.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is FqName) return false

        if (fqName != o.fqName) return false

        return true
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromSegments(names: List<String?>): FqName {
            return FqName(names.joinToString("."))
        }

        @JvmField
        val ROOT: FqName = FqName("")

        @JvmStatic
        fun topLevel(shortName: Name): FqName {
            return FqName(FqNameUnsafe.topLevel(shortName))
        }
    }
}
