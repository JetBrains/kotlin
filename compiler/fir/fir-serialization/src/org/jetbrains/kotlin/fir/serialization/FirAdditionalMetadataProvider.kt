/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnnotation

abstract class FirAdditionalMetadataProvider {
    abstract fun findGeneratedAnnotationsFor(declaration: FirDeclaration): List<FirAnnotation>
    abstract fun hasGeneratedAnnotationsFor(declaration: FirDeclaration): Boolean
}
