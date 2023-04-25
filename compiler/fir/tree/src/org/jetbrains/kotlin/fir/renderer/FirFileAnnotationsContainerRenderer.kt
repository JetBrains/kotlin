/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer

class FirFileAnnotationsContainerRenderer {
    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer

    fun render(fileAnnotationsContainer: FirFileAnnotationsContainer) {
        components.annotationRenderer?.render(fileAnnotationsContainer)
        components.resolvePhaseRenderer?.render(fileAnnotationsContainer)
        printer.println("annotations container")
    }
}