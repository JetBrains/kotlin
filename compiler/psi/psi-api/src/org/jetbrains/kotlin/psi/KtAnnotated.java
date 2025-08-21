/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an element that ows annotations (the element may or may not be the parent for the annotations).
 *
 * @see KtAnnotationsContainer
 */
public interface KtAnnotated extends KtElement {
    /**
     * Retrieves the list of {@link KtAnnotation} associated with this element.
     * <p/>
     * <b>Important</b>: this list contains only {@link KtAnnotation} and not {@link KtAnnotationEntry}.
     *
     * @see #getAnnotationEntries
     */
    @NotNull
    List<KtAnnotation> getAnnotations();

    /**
     * Retrieves the list of {@link KtAnnotationEntry} associated with this element.
     * <p/>
     * The list may contain directly declared {@link KtAnnotationEntry} or unwrapped entries from {@link KtAnnotation}.
     *
     * <h3>
     *     Example:
     * </h3>
     * <pre>{@code
     *     @[Anno1, Anno2] @Anno3
     *     fun foo() {
     *
     *     }
     * }</pre>
     *
     * The list will contain {@code Anno1}, {@code Anno2}, and {@code Anno3}.
     */
    @NotNull
    List<KtAnnotationEntry> getAnnotationEntries();
}
