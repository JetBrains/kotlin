/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.name.ClassId

private val jdkInternalValueBasedAnnotationClassId = ClassId.fromString("jdk/internal/ValueBased")

internal fun ConeKotlinType.isJavaValueBasedClass(session: FirSession): Boolean {
    val classSymbol = toClassSymbol(session) ?: return false
    return classSymbol.hasAnnotation(jdkInternalValueBasedAnnotationClassId, session)
}

internal fun ConeKotlinType.isJavaValueBasedClassAndWarningsEnabled(session: FirSession): Boolean {
    return !session.languageVersionSettings.supportsFeature(LanguageFeature.DisableWarningsForValueBasedJavaClasses)
            && this.isJavaValueBasedClass(session)
}

internal fun ConeKotlinType.isValueTypeAndWarningsEnabled(session: FirSession): Boolean {
    if (!session.languageVersionSettings.supportsFeature(LanguageFeature.DisableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives) &&
        (this.isPrimitiveOrNullablePrimitive || this.isValueClass(session) || this.isFlexiblePrimitive())
    ) return true
    return this.isJavaValueBasedClassAndWarningsEnabled(session)
}

internal fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}
