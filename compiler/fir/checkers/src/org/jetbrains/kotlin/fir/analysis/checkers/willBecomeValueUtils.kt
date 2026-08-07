/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirWillBecomeValueDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInlineOrValue
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * '@WillBecomeValue' marks a reference class which is going to become a full value class, so every value class
 * *declaration* check is run on it as an error, while identity-sensitive *usages* are only warnings outside of the
 * annotated class itself: the class is not a value class yet, and downstream code still needs time to migrate.
 */
private fun FirClassSymbol<*>.hasWillBecomeValueAnnotation(session: FirSession): Boolean =
    hasAnnotation(StandardClassIds.Annotations.WillBecomeValue, session)

/**
 * The presentable name of the target '@WillBecomeValue' cannot be applied to, or 'null' if it can be applied.
 *
 * A value class already has value semantics, interfaces are common for identity and value classes,
 * and neither enums nor 'open' classes are going to become value classes.
 */
fun FirRegularClassSymbol.willBecomeValueInapplicableTarget(): String? = when {
    isInlineOrValue -> "a value class"
    classKind == ClassKind.INTERFACE -> "an interface"
    classKind == ClassKind.ANNOTATION_CLASS -> "an annotation class"
    classKind == ClassKind.ENUM_CLASS -> "an enum class"
    modality == Modality.OPEN -> "an open class"
    else -> null
}

/**
 * 'true' if the class is annotated with '@WillBecomeValue' and the annotation is applicable to it.
 *
 * Inapplicable annotations are reported by [FirWillBecomeValueDeclarationChecker], and the value class declaration
 * checks are skipped for them: the class is not going to become a value class anyway.
 */
fun FirRegularClassSymbol.willBecomeValueClass(session: FirSession): Boolean =
    hasWillBecomeValueAnnotation(session) && willBecomeValueInapplicableTarget() == null

/**
 * 'true' if the class has promised to become a value class: either with '@WillBecomeValue', or, being a JDK class, with
 * '@jdk.internal.ValueBased', since JEP 401 migrates all value-based classes to value classes.
 *
 * Such a class still has identity at run time, so only another class which is going to lose its identity along with it
 * may extend it.
 */
fun FirRegularClassSymbol.promisedToBecomeValueClass(session: FirSession): Boolean =
    willBecomeValueClass(session) || hasAnnotation(JDK_INTERNAL_VALUE_BASED_ANNOTATION_CLASS_ID, session)

/**
 * Reports an identity-sensitive operation performed on [type] if that type is annotated with '@WillBecomeValue'.
 *
 * The author of the annotated class has already committed to value semantics, so inside it the operation is an error,
 * while outside it is only a migration warning. Called from both the common and the platform-specific identity checkers
 * so that every identity-sensitive operation they already know about is covered for '@WillBecomeValue' classes as well.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun reportIfWillBecomeValueClass(source: KtSourceElement?, type: ConeKotlinType) {
    val classSymbol = type.toRegularClassSymbol(context.session)?.takeIf { it.hasWillBecomeValueAnnotation(context.session) } ?: return
    val factory = when (classSymbol) {
        in context.containingDeclarations -> FirErrors.IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS_ERROR
        else -> FirErrors.IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS
    }
    reporter.reportOn(source, factory, type)
}
