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

    abstract fun findMetadataExtensionsFor(declaration: FirDeclaration): Map<String, ByteArray>
}

class CompoundAdditionalMetadataProvider(private val providers: List<FirAdditionalMetadataProvider>) : FirAdditionalMetadataProvider() {
    override fun findGeneratedAnnotationsFor(declaration: FirDeclaration): List<FirAnnotation> =
        providers.flatMap { it.findGeneratedAnnotationsFor(declaration) }

    override fun hasGeneratedAnnotationsFor(declaration: FirDeclaration): Boolean =
        providers.any { it.hasGeneratedAnnotationsFor(declaration) }

    override fun findMetadataExtensionsFor(declaration: FirDeclaration): Map<String, ByteArray> =
        providers.flatMap { it.findMetadataExtensionsFor(declaration).entries }
            .associate { it.key to it.value }
}
