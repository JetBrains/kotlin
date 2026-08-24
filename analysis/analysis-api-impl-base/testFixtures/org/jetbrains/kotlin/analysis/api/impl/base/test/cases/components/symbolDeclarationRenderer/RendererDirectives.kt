/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeQualification
import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.rendering.KaTypeApproximation
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

    val CLASS_TYPE_QUALIFICATION by enumDirective<KaClassTypeQualification>(
        description = "How qualified the names of class types are rendered (see [KaRenderingOption.ClassTypeQualification]).",
    )

    val CLASS_TYPE_RENDERING_MODE by enumDirective<KaClassTypeRenderingMode>(
        description = "How a class type which involves a type alias is rendered (see [KaRenderingOption.ClassTypeRenderingMode]).",
    )

    val TYPE_APPROXIMATION by enumDirective<KaTypeApproximation>(
        description = "Whether rendered types are approximated to denotable ones, and in which direction " +
                "(see [KaRenderingOption.TypeApproximation]).",
    )

    val NO_PRIMARY_CONSTRUCTOR_IN_CLASS_HEADER by directive(
        description = "Renders the primary constructor and the properties it declares as class body members instead of the class header.",
    )

    val NO_EXTRA_LINE_BETWEEN_MEMBERS by directive(
        description = "Renders consecutive members within a class body without a separating blank line.",
    )
}
