/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search

/**
 * An opaque set of files a lookup may see, with set algebra over it. The files themselves are never exposed:
 * a platform supplies its own implementation (`VirtualFile`-based in the CLI) and only that implementation
 * knows what a file is.
 */
interface AbstractProjectFileSearchScope {
    val isEmpty: Boolean

    operator fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope

    operator fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope

    operator fun not(): AbstractProjectFileSearchScope


    object EMPTY : AbstractProjectFileSearchScope {
        override val isEmpty: Boolean = true
        override fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = this
        override fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = other
        override fun not(): AbstractProjectFileSearchScope = ANY
    }

    object ANY : AbstractProjectFileSearchScope {
        override val isEmpty: Boolean = false
        override fun minus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = error("Operation not implemented")
        override fun plus(other: AbstractProjectFileSearchScope): AbstractProjectFileSearchScope = this
        override fun not(): AbstractProjectFileSearchScope = EMPTY
    }
}
