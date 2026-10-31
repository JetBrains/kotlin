/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeContext

/**
 * How instances of a type are value-like for identity-sensitive operations. Each check looks through the bounds of type parameters,
 * captured, intersection and flexible types (see [collectUpperBounds]); the first kind that applies is taken. '@WillBecomeValue' classes
 * are not value-like yet, see [reportIdentitySensitiveOperation].
 */
enum class IdentityKind {
    /** A Kotlin, Java or JDK value class, or a primitive. */
    VALUE_TYPE,

    /** A primitive type from Java, like `Int!`, which is its box. */
    FLEXIBLE_PRIMITIVE,

    /** A JDK class annotated with `@jdk.internal.ValueBased`. */
    VALUE_BASED,
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.identityKind(): IdentityKind? = when {
    isValueClassOrPrimitive() -> IdentityKind.VALUE_TYPE
    isFlexiblePrimitive() -> IdentityKind.FLEXIBLE_PRIMITIVE
    isJavaValueBasedClass() -> IdentityKind.VALUE_BASED
    else -> null
}

// A flexible primitive type like `Int!` is its Java box, like `java.lang.Integer`, so it is a value class only if its platform class is.
context(sessionHolder: SessionHolder)
fun ConeKotlinType.isValueClassOrPrimitive(): Boolean =
    if (isFlexiblePrimitive()) isValueClass(sessionHolder.session)
    else isBoundedByPrimitiveOrValueClass(sessionHolder.session)

/**
 * Reports an identity-sensitive operation on [type]: [reportValueLike] reports it for the [IdentityKind] of a value-like type, which is not
 * passed for value-based classes with their warnings disabled, and a type annotated with '@WillBecomeValue' gets
 * [reportIdentitySensitiveOperationIfWillBecomeValueClass].
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
inline fun reportIdentitySensitiveOperation(source: KtSourceElement?, type: ConeKotlinType, reportValueLike: (IdentityKind) -> Unit) {
    when (val kind = type.identityKind()) {
        null -> reportIdentitySensitiveOperationIfWillBecomeValueClass(source, type)
        IdentityKind.VALUE_BASED -> if (enableWarningsForValueBasedJavaClasses()) reportValueLike(kind)
        else -> reportValueLike(kind)
    }
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.isJavaValueBasedClass(): Boolean =
    collectUpperBounds(sessionHolder.session.typeContext).any {
        it.toClassSymbol()?.hasAnnotation(JDK_INTERNAL_VALUE_BASED_ANNOTATION_CLASS_ID, sessionHolder.session) == true
    }
