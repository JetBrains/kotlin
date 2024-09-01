/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.fir.expressions.FirAnnotation

class FirAnnotationSourceElement(val fir: FirAnnotation) : SourceElement {
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
