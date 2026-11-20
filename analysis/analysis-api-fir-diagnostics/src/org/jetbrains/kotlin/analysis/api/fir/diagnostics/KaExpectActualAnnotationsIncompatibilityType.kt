/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation

/**
 * The reason why the annotations of an `actual` declaration do not match the annotations of its `expect` declaration.
 */
@KaUnstableDiagnosticApi
public sealed class KaExpectActualAnnotationsIncompatibilityType {
    /**
     * The annotation of the `expect` declaration which has no match.
     */
    public abstract val expectAnnotation: KaAnnotation

    /**
     * The `actual` declaration has no annotation of the same class as [expectAnnotation].
     */
    @KaUnstableDiagnosticApi
    public class MissingOnActual(override val expectAnnotation: KaAnnotation) : KaExpectActualAnnotationsIncompatibilityType()

    /**
     * The `actual` declaration has an annotation of the same class as [expectAnnotation], but with different arguments.
     *
     * @property actualAnnotation The annotation of the `actual` declaration.
     */
    @KaUnstableDiagnosticApi
    public class DifferentOnActual(
        override val expectAnnotation: KaAnnotation,
        public val actualAnnotation: KaAnnotation,
    ) : KaExpectActualAnnotationsIncompatibilityType()
}
