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
import org.jetbrains.kotlin.fir.analysis.checkers.isValueClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isPrimitive
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
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
    )

    private val operationsToCheckFirstTypeArgCallableIds = setOf(
        CallableId(FqName("java.util"), FqName("IdentityHashMap"), Name.identifier("IdentityHashMap")),
        CallableId(FqName("java.util"), FqName("WeakHashMap"), Name.identifier("WeakHashMap")),
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val function = expression.calleeReference.toResolvedCallableSymbol() ?: return
        when (function.callableId) {
            synchronizedCallableId -> checkSynchronizedCall(expression)

            in operationsToCheckFirstArgCallableIds -> {
                val type = expression.arguments.firstOrNull()?.resolvedType ?: return
                if (type.isValueTypeAndWarningsEnabled()) {
                    reporter.reportOn(
                        expression.argument.source, FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type
                    )
                }
            }

            in operationsToCheckFirstTypeArgCallableIds -> {
                val typeArgument = expression.typeArguments.firstOrNull() as? FirTypeProjectionWithVariance ?: return
                val type = typeArgument.typeRef.coneType.upperBoundIfFlexible()
                if (type.isValueTypeAndWarningsEnabled()) {
                    reporter.reportOn(
                        typeArgument.source, FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type
                    )
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSynchronizedCall(
        expression: FirFunctionCall,
    ) {
        for ((argument, parameter) in expression.resolvedArgumentMapping?.entries ?: return) {
            if (parameter.name != lockParameterName) continue
            val type = argument.resolvedType
            if (type.isPrimitive || type.isValueClass(context.session)) {
                reporter.reportOn(argument.source, SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE, type)
            }
            if (type.isJavaValueBasedClassAndWarningsEnabled()) {
                reporter.reportOn(argument.source, SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS, type)
            }
            if (enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives() && type.isFlexiblePrimitive()) {
                reporter.reportOn(argument.source, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type)
            }
        }
    }
}
