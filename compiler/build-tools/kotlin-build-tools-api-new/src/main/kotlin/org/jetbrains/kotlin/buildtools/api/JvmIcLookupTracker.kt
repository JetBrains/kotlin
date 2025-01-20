/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

//@InternalBuildToolsApi
public interface JvmIcLookupTracker {
    public class Position(public val line: Int, public val column: Int)

    public enum class ScopeKind {
        PACKAGE,
        CLASSIFIER
    }

    public fun recordLookup(
        filePath: String,
        position: Position,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String
    )
}