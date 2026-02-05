/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib.compatibility

class TestVersion(val basicVersion: KotlinVersion, val postfix: String) : Comparable<TestVersion> {
    constructor(major: Int, minor: Int, patch: Int, postfix: String = "") : this(KotlinVersion(major, minor, patch), postfix)

    override fun compareTo(other: TestVersion) = basicVersion.compareTo(other.basicVersion)
    override fun equals(other: Any?) = (other as? TestVersion)?.basicVersion == basicVersion
    override fun hashCode() = basicVersion.hashCode()
    override fun toString() = basicVersion.toString() + postfix
}