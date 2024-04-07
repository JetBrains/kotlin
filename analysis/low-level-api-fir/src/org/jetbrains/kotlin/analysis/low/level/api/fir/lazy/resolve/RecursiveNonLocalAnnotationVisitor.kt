/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirScript

/**
 * In opposite to [NonLocalAnnotationVisitor] processes not only the target declaration,
 * but also its nested declarations.
 *
 * @see NonLocalAnnotationVisitor
 */
internal abstract class RecursiveNonLocalAnnotationVisitor<T> : NonLocalAnnotationVisitor<T>() {
    override fun visitFile(file: FirFile, data: T) {
        super.visitFile(file, data)

        file.forEachDeclaration { it.accept(this, data) }
    }

    override fun visitScript(script: FirScript, data: T) {
        super.visitScript(script, data)

        script.forEachDeclaration { it.accept(this, data) }
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: T) {
        super.visitRegularClass(regularClass, data)

        regularClass.forEachDeclaration { it.accept(this, data) }
    }
}
