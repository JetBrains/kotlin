/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.isJvmTargetValhallaCompatible
import org.jetbrains.kotlin.config.isValhallaSupportEnabled
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.anyBound
import org.jetbrains.kotlin.fir.analysis.checkers.isMappedToJavaValueClass
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.declaresValueClassInClassFile
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.isJavaValueClass
import org.jetbrains.kotlin.fir.java.jvmTargetProvider
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.name.ClassId

private val jdkInternalValueBasedAnnotationClassId = ClassId.fromString("jdk/internal/ValueBased")

// Includes type parameters and captured types bounded by a value-based class, like value classes do.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueBasedClass(): Boolean =
    anyBound { it.toClassSymbol()?.hasAnnotation(jdkInternalValueBasedAnnotationClassId, sessionHolder.session) == true }

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueBasedClassAndWarningsEnabled(): Boolean {
    return enableWarningsForValueBasedJavaClasses() && this.isJavaValueBasedClass()
}

context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueTypeAndWarningsEnabled(): Boolean {
    if (enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives() &&
        (this.isFlexiblePrimitive() || this.isValueClassOrPrimitive())
    ) return true
    return this.isJavaValueBasedClassAndWarningsEnabled()
}

// Whether instances of this type are value objects at run time (JEP 401): with a Valhalla-compatible JVM target, boxed primitives and
// Java value classes are, and so are Kotlin value classes compiled as Valhalla value classes, as their class file or the module declaring
// them says.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueObjectAtRuntime(): Boolean {
    val session = sessionHolder.session
    val jvmTarget = session.jvmTargetProvider?.jvmTarget ?: return false
    if (!isJvmTargetValhallaCompatible(jvmTarget, session.languageVersionSettings.getFlag(JvmAnalysisFlags.enableJvmPreview))) return false
    if (isFlexiblePrimitive()) return true
    return anyBound {
        val symbol = it.toRegularClassSymbol(session)
        it.isPrimitiveOrNullablePrimitive || symbol?.isJavaValueClass(session) == true ||
                symbol?.isInlineOrValue == true &&
                (symbol.declaresValueClassInClassFile() ?: symbol.moduleData.session.languageVersionSettings.isValhallaSupportEnabled())
    }
}

// Like javac, this includes type parameters and captured types bounded by a primitive or a value class. A flexible primitive type like
// `Int!` is its Java box, like `java.lang.Integer`.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isValueClassOrPrimitive(): Boolean =
    if (isFlexiblePrimitive()) isJavaValueClass()
    else anyBound { it.isPrimitiveOrNullablePrimitive || it.isValueClass(sessionHolder.session) }

// Like javac, this includes type parameters and captured types bounded by a Java value class. A flexible primitive type like `Int!` is
// its Java box, like `java.lang.Integer`.
context(sessionHolder: SessionHolder)
internal fun ConeKotlinType.isJavaValueClass(): Boolean =
    if (isFlexiblePrimitive()) lowerBoundIfFlexible().toRegularClassSymbol()?.isMappedToJavaValueClass(sessionHolder.session) == true
    else anyBound { it.toRegularClassSymbol()?.isJavaValueClass(sessionHolder.session) == true }

internal fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}
