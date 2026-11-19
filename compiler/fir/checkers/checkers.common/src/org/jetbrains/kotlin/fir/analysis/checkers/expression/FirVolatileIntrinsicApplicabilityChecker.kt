/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassIds
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.StandardClassIds.Annotations

/**
 * Checks calls of the atomic intrinsics on volatile properties, e.g. `this::counter.compareAndSet(0, 1)`
 * (see `kotlin/concurrent/atomics/VolatilePropertyAtomics.common.kt`).
 *
 * Similarly to `isInitialized` (see [FirLateinitIntrinsicApplicabilityChecker]), such intrinsics are recognized by the
 * `@receiver:kotlin.internal.AccessibleVolatilePropertyLiteral` annotation. Backends replace the calls with atomic operations
 * on the backing field of the referenced property, so the receiver must be a property reference literal, referencing
 * a property with a `@Volatile` backing field, which is accessible at the call site, and isn't stored in a static field.
 */
object FirVolatileIntrinsicApplicabilityChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val calleeSymbol = expression.calleeReference.toResolvedNamedFunctionSymbol() ?: return
        val receiverParameter = calleeSymbol.receiverParameterSymbol ?: return
        if (!receiverParameter.hasAnnotation(Annotations.AccessibleVolatilePropertyLiteral, context.session)) return

        val source = expression.calleeReference.source

        val extensionReceiver = expression.extensionReceiver
        if (extensionReceiver !is FirCallableReferenceAccess) {
            reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_ON_NON_LITERAL)
            return
        }

        val referencedSymbol = extensionReceiver.calleeReference.toResolvedVariableSymbol() ?: return
        val propertySymbol = (referencedSymbol as? FirPropertySymbol)?.unwrapFakeOverrides()
        val volatileAnnotations = context.session.annotationPlatformSupport.volatileAnnotations
        val volatileAnnotation = propertySymbol?.backingFieldSymbol?.resolvedAnnotationsWithClassIds
            ?.getAnnotationByClassIds(volatileAnnotations, context.session)
        if (propertySymbol == null || volatileAnnotation == null || propertySymbol.delegateFieldSymbol != null) {
            reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_ON_NON_VOLATILE, referencedSymbol)
            return
        }

        // Atomic operations work with the backing field directly, bypassing accessors.
        // Prohibit custom accessors, so that the behavior is the same regardless of the backend.
        if (propertySymbol.getterSymbol?.isDefault == false || propertySymbol.setterSymbol?.isDefault == false) {
            reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_ON_PROPERTY_WITH_CUSTOM_ACCESSORS, propertySymbol)
            return
        }

        // Top-level properties and properties of (companion) objects are stored in static fields on the JVM, which can't be accessed
        // atomically by JVM 1.8 `Atomic*FieldUpdater`s. To keep the API consistent across platforms and JVM targets, such properties
        // are prohibited everywhere: they are allocated once, so `Atomic*` classes (boxed atomics) are cheap there.
        // This can be reconsidered when JVM 1.8 support is dropped, since `VarHandle` supports static fields.
        if (propertySymbol.isStoredInStaticField()) {
            reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_ON_STATIC_PROPERTY, propertySymbol)
            return
        }

        // The generic (`AtomicReference`-like) overloads compare values by identity, which is meaningless for primitives
        // and value classes. Int/Long/Boolean have dedicated overloads which are preferred by overload resolution.
        if (calleeSymbol.typeParameterSymbols.isNotEmpty()) {
            val propertyType = propertySymbol.resolvedReturnType.fullyExpandedType()
            if (propertyType.isPrimitiveOrNullablePrimitive || propertyType.isValueClass(context.session)) {
                reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_ON_UNSUPPORTED_TYPE, propertyType)
                return
            }
        }

        // The property must be declared in one of the outer lexical scopes: backends access its backing field directly.
        val containingSymbol = propertySymbol.containingClassOrFile()
        if (context.containingDeclarations.none { it == containingSymbol }) {
            reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY, propertySymbol)
            return
        }

        // The lowered code touches private (synthetic) members of the declaring class, so it must not be inlined elsewhere.
        // Unlike `isInitialized`, this also includes lambdas and local declarations inside inline functions.
        if (context.containingDeclarations.any { it is FirFunctionSymbol && it.isInline }) {
            reporter.reportOn(source, FirErrors.VOLATILE_INTRINSIC_CALL_IN_INLINE_FUNCTION)
        }
    }

    /**
     * Whether the property is top-level or declared in an object (including companion objects, excluding anonymous objects).
     */
    private fun FirPropertySymbol.isStoredInStaticField(): Boolean {
        val containingClass = getContainingClassSymbol() ?: return true
        return containingClass is FirRegularClassSymbol && containingClass.classKind == ClassKind.OBJECT
    }

    /**
     * Returns the containing class or file if the property is top-level.
     */
    context(context: CheckerContext)
    private fun FirPropertySymbol.containingClassOrFile(): FirBasedSymbol<*>? {
        return getContainingClassSymbol()
            ?: context.session.firProvider.getFirCallableContainerFile(this)?.symbol
    }
}
