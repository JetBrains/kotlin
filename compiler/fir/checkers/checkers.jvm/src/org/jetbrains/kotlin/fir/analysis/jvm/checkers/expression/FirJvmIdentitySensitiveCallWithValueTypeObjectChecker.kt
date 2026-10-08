/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isFlexiblePrimitive
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.checkers.reportIdentitySensitiveOperationIfWillBecomeValueClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.fir.types.isPrimitiveOrNullablePrimitive
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirJvmIdentitySensitiveCallWithValueTypeObjectChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val synchronizedCallableId = CallableId(FqName("kotlin"), Name.identifier("synchronized"))
    private val lockParameterName = Name.identifier("lock")

    private val operationsToCheckFirstArgCallableIds = setOf(
        CallableId(FqName("java.lang"), FqName("System"), Name.identifier("identityHashCode")),
        CallableId(FqName("java.lang.ref"), FqName("Cleaner"), Name.identifier("register")),
        CallableId(FqName("java.lang.ref"), FqName("PhantomReference"), Name.identifier("PhantomReference")),
        CallableId(FqName("java.lang.ref"), FqName("SoftReference"), Name.identifier("SoftReference")),
        CallableId(FqName("java.lang.ref"), FqName("WeakReference"), Name.identifier("WeakReference")),
        CallableId(FqName("java.lang.ref"), FqName("Reference"), Name.identifier("refersTo")),
    )

    private val operationsToCheckFirstTypeArgCallableIds = setOf(
        CallableId(FqName("java.util"), FqName("IdentityHashMap"), Name.identifier("IdentityHashMap")),
        CallableId(FqName("java.util"), FqName("WeakHashMap"), Name.identifier("WeakHashMap")),
    )

    private val atomicFqName = FqName("java.util.concurrent.atomic")

    private val compareAndSetNames = listOf("compareAndSet", "weakCompareAndSet")

    private val varHandleCompareAndSetNames = listOf(
        "compareAndSet", "compareAndExchange", "compareAndExchangeAcquire", "compareAndExchangeRelease",
        "weakCompareAndSet", "weakCompareAndSetPlain", "weakCompareAndSetAcquire", "weakCompareAndSetRelease",
    )

    private val stampedAndMarkableReferenceClassNames = listOf("AtomicStampedReference", "AtomicMarkableReference")

    // The arguments that field updaters, variable handles and stamped or markable references compare with the current value by
    // identity: the expected and the new value.
    private val comparedArguments: Map<CallableId, (List<FirExpression>) -> List<FirExpression>> = buildMap {
        for (name in compareAndSetNames) {
            this[CallableId(atomicFqName, FqName("AtomicReferenceFieldUpdater"), Name.identifier(name))] = { it.takeLast(2) }
        }
        for (name in varHandleCompareAndSetNames) {
            this[CallableId(FqName("java.lang.invoke"), FqName("VarHandle"), Name.identifier(name))] = { it.takeLast(2) }
        }
        for (className in stampedAndMarkableReferenceClassNames) {
            for (name in compareAndSetNames) {
                this[CallableId(atomicFqName, FqName(className), Name.identifier(name))] = { it.take(2) }
            }
        }
        this[CallableId(atomicFqName, FqName("AtomicStampedReference"), Name.identifier("attemptStamp"))] = { it.take(1) }
        this[CallableId(atomicFqName, FqName("AtomicMarkableReference"), Name.identifier("attemptMark"))] = { it.take(1) }
    }

    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val function = expression.calleeReference.toResolvedCallableSymbol() ?: return
        // 'Reference.refersTo' is inherited by the reference classes, so a function is matched by its original declaration.
        val callableId = function.unwrapFakeOverrides().callableId
        comparedArguments[callableId]?.let { select ->
            // The arguments are selected by position: these are Java methods, so they have neither named nor optional arguments.
            // The arguments of a 'VarHandle' method are passed as varargs.
            for (argument in select(expression.arguments.flatMap { (it as? FirVarargArgumentsExpression)?.arguments ?: listOf(it) })) {
                checkComparedValue(argument)
            }
            return
        }
        when (callableId) {
            synchronizedCallableId -> checkSynchronizedCall(expression)

            in operationsToCheckFirstArgCallableIds -> {
                val argument = expression.arguments.firstOrNull() ?: return
                val type = argument.resolvedType
                if (type.isValueTypeAndWarningsEnabled()) {
                    reporter.reportOn(
                        argument.source, FirErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type
                    )
                }
                reportIdentitySensitiveOperationIfWillBecomeValueClass(argument.source, type)
            }

            in operationsToCheckFirstTypeArgCallableIds -> {
                val typeArgument = expression.typeArguments.firstOrNull() as? FirTypeProjectionWithVariance ?: return
                val type = typeArgument.typeRef.coneType.upperBoundIfFlexible()
                if (type.isValueTypeAndWarningsEnabled()) {
                    reporter.reportOn(
                        typeArgument.source, FirErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type
                    )
                }
                reportIdentitySensitiveOperationIfWillBecomeValueClass(typeArgument.source, type)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkComparedValue(argument: FirExpression) {
        val type = argument.resolvedType
        if (type.lowerBoundIfFlexible().isPrimitiveOrNullablePrimitive || type.isValueClass(context.session) ||
            type.isJavaValueBasedClassAndWarningsEnabled()
        ) {
            reporter.reportOn(argument.source, FirErrors.ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY, type)
        }
        reportIdentitySensitiveOperationIfWillBecomeValueClass(argument.source, type)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSynchronizedCall(
        expression: FirFunctionCall,
    ) {
        for ([argument, parameter] in expression.resolvedArgumentMapping?.entries ?: return) {
            if (parameter.name != lockParameterName) continue
            val type = argument.resolvedType
            if (type.isPrimitive || type.isValueClass(context.session)) {
                reporter.reportOn(argument.source, SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE, type)
            } else if (type.isJavaValueBasedClassAndWarningsEnabled()) {
                reporter.reportOn(argument.source, SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS, type)
            } else if (enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives() && type.isFlexiblePrimitive()) {
                reporter.reportOn(argument.source, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type)
            }
            reportIdentitySensitiveOperationIfWillBecomeValueClass(argument.source, type)
        }
    }
}
