/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.allopen

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.name.FqName

class AllOpenStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun transformStatus(declaration: FirDeclaration, status: FirDeclarationStatus): FirDeclarationStatus {
        if (status.modality != null) return status
        return status.transform(modality = Modality.OPEN)
    }

    override val mode: Mode
        get() = Mode.ALL_IN_ANNOTATED_ELEMENT

    override val annotations: Set<AnnotationFqn> = setOf(FqName.fromSegments(listOf("allopen", "Open")))

    override val metaAnnotations: Set<AnnotationFqn>
        get() = emptySet()
}