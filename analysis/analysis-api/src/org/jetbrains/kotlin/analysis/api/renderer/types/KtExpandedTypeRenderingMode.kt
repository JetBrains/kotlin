/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types

/**
 * [KaExpandedTypeRenderingMode] controls how [KaTypeRenderer] renders expanded types (with an associated abbreviated type) and type aliases
 * which can still be expanded.
 */
public enum class KaExpandedTypeRenderingMode {
    /**
     * Renders only the abbreviated type as-is, e.g. `foo.bar.StringAlias`.
     */
    RENDER_ABBREVIATED_TYPE,

    /**
     * Renders the abbreviated type as-is and its expansion in a comment, e.g. `foo.bar.StringAlias /* = kotlin.String */`.
     */
    RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT,

    /**
     * Renders the expanded type as-is, e.g. `kotlin.String`.
     */
    RENDER_EXPANDED_TYPE,

    /**
     * Renders the expanded type as-is and its abbreviated type in a comment, e.g. `kotlin.String /* from: foo.bar.StringAlias */`.
     */
    RENDER_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT,
}

public typealias KtExpandedTypeRenderingMode = KaExpandedTypeRenderingMode