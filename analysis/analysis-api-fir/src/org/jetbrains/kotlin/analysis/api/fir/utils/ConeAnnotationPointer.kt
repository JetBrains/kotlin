/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.fir.computeTypeAttributes
import org.jetbrains.kotlin.fir.types.*

/**
 * A pointer which restores [ConeKotlinType.typeAnnotations].
 *
 * Annotations which cannot be represented by a [FirAnnotationPointer] are dropped, so the restored type may have fewer annotations
 * than the original one.
 */
internal class ConeAnnotationPointer private constructor(private val pointers: List<FirAnnotationPointer>) {
    fun restore(session: KaFirSession, guard: ConeTypeRecursionGuard): ConeAttributes {
        val annotations = pointers.mapNotNull { it.restore(session, guard) }
        if (annotations.isEmpty()) {
            return ConeAttributes.Empty
        }

        return annotations.computeTypeAttributes(session.firSession, shouldExpandTypeAliases = true)
    }

    companion object {
        private val EMPTY = ConeAnnotationPointer(emptyList())

        fun create(coneType: ConeKotlinType, builder: KaSymbolByFirBuilder, guard: ConeTypeRecursionGuard): ConeAnnotationPointer {
            val annotations = coneType.typeAnnotations.ifEmpty {
                return EMPTY
            }

            val pointers = annotations.mapNotNull { FirAnnotationPointer.create(it, builder, guard) }.ifEmpty {
                return EMPTY
            }

            return ConeAnnotationPointer(pointers)
        }
    }
}
