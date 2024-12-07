/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds

fun FirRegularClassSymbol.isOptionalAnnotationClass(session: FirSession): Boolean {
    return classKind == ClassKind.ANNOTATION_CLASS &&
            isExpect &&
            hasAnnotation(StandardClassIds.Annotations.OptionalExpectation, session)
}