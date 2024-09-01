/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.filterOutAnnotationsByClassId
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

// See K1 counterpart at org.jetbrains.kotlin.resolve.FunctionDescriptorResolver.removeParameterNameAnnotation
fun ConeKotlinType.removeParameterNameAnnotation(session: FirSession): ConeKotlinType {
    // Fast-path
    val custom = attributes.custom ?: return this
    if (customAnnotations.getAnnotationByClassId(StandardNames.FqNames.parameterNameClassId, session) == null) return this

    val newAnnotations = custom.annotations.filterOutAnnotationsByClassId(StandardNames.FqNames.parameterNameClassId, session)
    return withAttributes(
        attributes.remove(custom).applyIf(newAnnotations.isNotEmpty()) {
            add(CustomAnnotationTypeAttribute(newAnnotations))
        }
    )
}
