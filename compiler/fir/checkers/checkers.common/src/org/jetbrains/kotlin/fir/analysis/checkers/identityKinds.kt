/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.enableWarningsForValueBasedJavaClasses
import org.jetbrains.kotlin.fir.isPlatformValueClass
import org.jetbrains.kotlin.fir.isValueBasedClass
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.platformClassMapper
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.typeContext

/**
 * How instances of a type are value-like for identity-sensitive operations. Each check looks through the bounds of type parameters,
 * captured, intersection and flexible types (see [collectUpperBounds]); the kinds are declared from the strictest, and the first one that
 * applies is taken.
 */
enum class IdentityKind {
    /** A Kotlin, Java or JDK value class, or a primitive. */
    VALUE_TYPE,

    /** A primitive type from Java, like `Int!`, which is its box. */
    FLEXIBLE_PRIMITIVE,

    /** A value-based class of the platform, like a JDK class annotated with `@jdk.internal.ValueBased`. */
    VALUE_BASED,

    /** A class annotated with '@WillBecomeValue', which is not a value class yet. */
    WILL_BECOME_VALUE,
}

context(sessionHolder: SessionHolder)
fun ConeKotlinType.identityKind(): IdentityKind? {
    val session = sessionHolder.session
    return when {
        // A flexible primitive type like `Int!` is its Java box, like `java.lang.Integer`, so it is a value class only if its platform
        // class is.
        isFlexiblePrimitive() -> if (isValueClass(session)) IdentityKind.VALUE_TYPE else IdentityKind.FLEXIBLE_PRIMITIVE
        isBoundedByPrimitiveOrValueClass(session) -> IdentityKind.VALUE_TYPE
        collectUpperBounds(session.typeContext).any { it.toRegularClassSymbol(session)?.isValueBasedClass(session) == true } ->
            IdentityKind.VALUE_BASED
        willBecomeValueTypeOf(this) != null -> IdentityKind.WILL_BECOME_VALUE
        else -> null
    }
}

/**
 * Reports an identity-sensitive operation on [type]: [reportValueLike] reports it for the [IdentityKind] of a value-like type, which is not
 * passed for value-based classes with their warnings disabled, and a type annotated with '@WillBecomeValue' gets
 * [FirErrors.IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS].
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
inline fun reportIdentitySensitiveOperation(source: KtSourceElement?, type: ConeKotlinType, reportValueLike: (IdentityKind) -> Unit) {
    when (val kind = type.identityKind()) {
        null -> {}
        IdentityKind.WILL_BECOME_VALUE -> reportIdentitySensitiveOperationOnWillBecomeValueClass(source, type)
        IdentityKind.VALUE_BASED -> if (enableWarningsForValueBasedJavaClasses()) reportValueLike(kind)
        else -> reportValueLike(kind)
    }
}

private fun ConeKotlinType.isKotlinValueClass(session: FirSession): Boolean {
    // Value classes have `inline` or `value` modifier in FIR
    return toRegularClassSymbol(session)?.isValue == true
}

/**
 * Whether this type is a value class type: a Kotlin inline or full value class, or a value class type of the platform
 * (see [isPlatformValueClassType]).
 */
fun ConeKotlinType.isValueClass(session: FirSession): Boolean =
    isKotlinValueClass(session) || isPlatformValueClassType(session)

/**
 * Whether this type is a value class type of the platform, like a Java value class. Like javac, this includes type parameters, captured
 * types and intersection types bounded by such a class. A Kotlin class whose platform class is a value class, like `kotlin.Number` for
 * `java.lang.Number` or `kotlin.Int` for `java.lang.Integer`, is one too.
 *
 * Unlike [FirRegularClassSymbol.isPlatformValueClass], which checks only the class itself.
 */
private fun ConeKotlinType.isPlatformValueClassType(session: FirSession): Boolean =
    collectUpperBounds(session.typeContext).any { type ->
        type.toRegularClassSymbol(session)?.let { it.isPlatformValueClass(session) || it.isMappedToPlatformValueClass(session) } == true
    }

fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}

fun FirRegularClassSymbol.isMappedToPlatformValueClass(session: FirSession): Boolean {
    val platformClassId = session.platformClassMapper.getCorrespondingPlatformClass(classId) ?: return false
    return (platformClassId.toSymbol(session) as? FirRegularClassSymbol)?.isPlatformValueClass(session) == true
}

context(context: CheckerContext)
internal val ConeKotlinType.hasStableIdentityForAtomicOperations: Boolean
    get() = when (fullyExpandedType().identityKind()) {
        IdentityKind.VALUE_TYPE, IdentityKind.FLEXIBLE_PRIMITIVE -> false
        IdentityKind.VALUE_BASED -> !enableWarningsForValueBasedJavaClasses()
        IdentityKind.WILL_BECOME_VALUE, null -> true
    }

fun ConeKotlinType.isBoundedByPrimitiveOrValueClass(session: FirSession): Boolean =
    collectUpperBounds(session.typeContext).any { it.isPrimitiveOrNullablePrimitive || it.isValueClass(session) }
