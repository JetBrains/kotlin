/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

/**
 * Directives which configure the renderer of [AbstractRendererTest].
 *
 * The directives are registered by [AbstractLegacyRenderingTest] as well, as both tests run on the same test data, but they only affect the
 * renderer of [AbstractRendererTest].
 */
internal object RendererDirectives : SimpleDirectivesContainer() {
    val NO_FLEXIBLE_TYPE_SHRINKING by directive(
        description = "Renders a flexible type in its full `(lower..upper)` form instead of the compact `lower!` form.",
    )
}
