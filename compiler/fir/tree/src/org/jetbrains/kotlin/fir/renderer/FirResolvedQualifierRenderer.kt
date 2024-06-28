/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier

open class FirResolvedQualifierRenderer {

    internal lateinit var components: FirRendererComponents

    protected val printer: FirPrinter get() = components.printer

    internal fun render(resolvedQualifier: FirResolvedQualifier) {
        components.annotationRenderer?.render(resolvedQualifier)
        renderResolvedQualifierWithoutAnnotations(resolvedQualifier)
    }

    protected open fun renderResolvedQualifierWithoutAnnotations(resolvedQualifier: FirResolvedQualifier) {
        val classId = resolvedQualifier.classId
        if (classId != null) {
            components.idRenderer.renderClassId(classId)
        } else {
            printer.print(resolvedQualifier.packageFqName.asString().replace(".", "/"))
        }
        if (resolvedQualifier.isNullableLHSForCallableReference) {
            printer.print("?")
        }
    }
}

class FirResolvedQualifierRendererWithLabel : FirResolvedQualifierRenderer() {
    override fun renderResolvedQualifierWithoutAnnotations(resolvedQualifier: FirResolvedQualifier) {
        printer.print("Q|")
        super.renderResolvedQualifierWithoutAnnotations(resolvedQualifier)
        printer.print("|")
    }
}