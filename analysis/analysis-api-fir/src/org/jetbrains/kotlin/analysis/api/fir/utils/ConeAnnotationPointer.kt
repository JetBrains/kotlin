/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.computeTypeAttributes
import org.jetbrains.kotlin.fir.types.*

/**
 * A pointer which restores [ConeKotlinType.customAnnotations].
 *
 * Annotations which cannot be represented by a [FirAnnotationPointer] are dropped, so the restored type may have fewer annotations
 * than the original one.
 */
internal class ConeAnnotationPointer private constructor(private val pointers: List<FirAnnotationPointer>) {
    fun restore(session: KaFirSession): ConeAttributes {
        val annotations = pointers.mapNotNull { it.restore(session) }
        if (annotations.isEmpty()) {
            return ConeAttributes.Empty
        }

        return annotations.computeTypeAttributes(session.firSession, shouldExpandTypeAliases = true)
    }

    companion object {
        fun create(coneType: ConeKotlinType, firSession: FirSession): ConeAnnotationPointer {
            val pointers = coneType.typeAnnotations.mapNotNull { FirAnnotationPointer.create(it, firSession) }

            return ConeAnnotationPointer(pointers)
        }
    }
}
