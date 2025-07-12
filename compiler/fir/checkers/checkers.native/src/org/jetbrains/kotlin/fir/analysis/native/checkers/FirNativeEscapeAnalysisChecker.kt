/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.backend.konan.BinaryType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.FirInlineClassesSupport
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeRuntimeNames
import org.jetbrains.kotlin.native.internal.Escapes
import org.jetbrains.kotlin.native.internal.IntrinsicType
import org.jetbrains.kotlin.native.internal.PointsTo
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

@OptIn(SymbolInternals::class)
object FirNativeEscapeAnalysisChecker : FirFunctionChecker(MppCheckerKind.Common) {

    // Annotation ClassIds
    private val escapesClassId = NativeRuntimeNames.Annotations.Escapes
    private val escapesNothingClassId = NativeRuntimeNames.Annotations.EscapesNothing
    private val pointsToClassId = NativeRuntimeNames.Annotations.PointsTo
    private val hasFinalizerClassId = NativeRuntimeNames.Annotations.HasFinalizer
    private val typedIntrinsicClassId = ClassId.fromString("kotlin/native/internal/TypedIntrinsic")

    // Package names
    private val kotlinPackageFqn = FqName("kotlin")
    private val kotlinNativeInternalPackage = FqName("kotlin.native.internal")
    private val kotlinConcurrentFqn = kotlinPackageFqn.child(Name.identifier("concurrent"))
    private val kotlinNativeConcurrentFqn = kotlinPackageFqn.child(Name.identifier("native")).child(Name.identifier("concurrent"))

    // DFGBuilder handled symbols
    private val symbolNamesHandledByDFG = setOf(
        "createUninitializedInstance",
        "createUninitializedArray",
        "createEmptyString",
        "reinterpret",
        "initInstance"
    )

    // Intrinsics that must be lowered
    private val intrinsicsThatMustBeLowered = setOf(
        IntrinsicType.ATOMIC_GET_FIELD,
        IntrinsicType.ATOMIC_SET_FIELD,
        IntrinsicType.GET_CONTINUATION,
        IntrinsicType.RETURN_IF_SUSPENDED,
        IntrinsicType.SAVE_COROUTINE_STATE,
        IntrinsicType.RESTORE_COROUTINE_STATE,
        IntrinsicType.INTEROP_BITS_TO_FLOAT,
        IntrinsicType.INTEROP_BITS_TO_DOUBLE,
        IntrinsicType.INTEROP_SIGN_EXTEND,
        IntrinsicType.INTEROP_NARROW,
        IntrinsicType.INTEROP_STATIC_C_FUNCTION,
        IntrinsicType.INTEROP_FUNPTR_INVOKE,
        IntrinsicType.INTEROP_CONVERT,
        IntrinsicType.ENUM_VALUES,
        IntrinsicType.ENUM_VALUE_OF,
        IntrinsicType.ENUM_ENTRIES,
        IntrinsicType.WORKER_EXECUTE,
        IntrinsicType.COMPARE_AND_SET_FIELD,
        IntrinsicType.COMPARE_AND_EXCHANGE_FIELD,
        IntrinsicType.GET_AND_SET_FIELD,
        IntrinsicType.GET_AND_ADD_FIELD
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (declaration !is FirSimpleFunction) return
        // Skip compiler-generated functions
        if (declaration.origin != FirDeclarationOrigin.Source) return

        checkEscapeAnalysisAnnotations(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEscapeAnalysisAnnotations(declaration: FirSimpleFunction) {
        val escapesAnnotation = declaration.getAnnotationByClassId(escapesClassId, context.session)
        val escapesNothingAnnotation = declaration.getAnnotationByClassId(escapesNothingClassId, context.session)
        val pointsToAnnotation = declaration.getAnnotationByClassId(pointsToClassId, context.session)

        val hasEscapes = escapesAnnotation != null
        val hasEscapesNothing = escapesNothingAnnotation != null
        val hasPointsTo = pointsToAnnotation != null

        fun warnUnusedIf(condition: Boolean, message: () -> String): Any? {
            if (!condition)
                return Unit
            if (hasEscapes) {
                reporter.reportOn(escapesAnnotation.source, FirNativeErrors.UNUSED_ESCAPES_ANNOTATION, message())
            }
            if (hasEscapesNothing) {
                reporter.reportOn(escapesNothingAnnotation.source, FirNativeErrors.UNUSED_ESCAPES_NOTHING_ANNOTATION, message())
            }
            if (hasPointsTo) {
                reporter.reportOn(pointsToAnnotation.source, FirNativeErrors.UNUSED_POINTS_TO_ANNOTATION, message())
            }
            // condition satisfied, potential unused warning emitted, no need to go on with other checks
            return null
        }

        warnUnusedIf(!declaration.isExternal) { "non-external function" } ?: return

        val packageFqName = declaration.symbol.callableId.packageName
        warnUnusedIf(!isPackageSupportedByEscapeAnalysis(packageFqName)) { "package outside EA annotation checks" } ?: return
        warnUnusedIf(isFunctionHandledByDFG(declaration)) { "function handled manually in DFGBuilder" } ?: return
        warnUnusedIf(isFunctionLoweredIntrinsic(declaration)) { "function is lowered in the compiler" } ?: return

        val signatureElements = collectSignatureElements(declaration)

        warnUnusedIf(signatureElements.all { it.type.cannotEscape(context.session) }) { "all of function parameters, receivers and the return value types cannot escape to the heap" }
            ?: return

        // All the unused checks have passed.
        // This also means, that we now know the declaration is external, in the correct package and so on.
        when {
            hasEscapes && hasEscapesNothing -> {
                reporter.reportOn(declaration.source, FirNativeErrors.CONFLICTING_ESCAPES_AND_ESCAPES_NOTHING)
            }
            !hasEscapes && signatureElements.any { it.type.mustEscape(context.session) } -> {
                reporter.reportOn(declaration.source, FirNativeErrors.MISSING_ESCAPES_FOR_MUST_ESCAPE_TYPE)
            }
            !hasEscapes && !hasEscapesNothing && !hasPointsTo -> {
                reporter.reportOn(declaration.source, FirNativeErrors.MISSING_ESCAPE_ANALYSIS_ANNOTATION)
            }
        }

        // Check annotation values
        if (hasEscapes || hasEscapesNothing) {
            val annotation = escapesAnnotation ?: escapesNothingAnnotation!!
            checkEscapesAnnotation(annotation, signatureElements, isEscapesNothing = hasEscapesNothing)
        }

        pointsToAnnotation?.let {
            checkPointsToAnnotation(it, signatureElements)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEscapesAnnotation(
        annotation: FirAnnotation,
        signatureElements: List<SignatureElement>,
        isEscapesNothing: Boolean,
    ) {
        val escapesValue = try {
            if (isEscapesNothing) {
                // @Escapes.Nothing means nothing escapes (mask = 0)
                Escapes(0, signatureElements.size)
            } else {
                getEscapesValue(annotation, context.session, signatureElements.size)
            }
        } catch (e: IllegalArgumentException) {
            reporter.reportOn(annotation.source, FirNativeErrors.INVALID_ESCAPES_VALUE, e.message ?: "")
            return
        } ?: return

        signatureElements.forEachIndexed { index, element ->
            if (escapesValue.escapesAt(index)) {
                if (element.type.cannotEscape(context.session)) {
                    reporter.reportOn(annotation.source, FirNativeErrors.ESCAPES_MARKED_ON_NON_ESCAPING_TYPE, element.name)
                }
            } else {
                if (element.type.mustEscape(context.session)) {
                    reporter.reportOn(annotation.source, FirNativeErrors.ESCAPES_NOT_MARKED_ON_MUST_ESCAPE_TYPE, element.name)
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPointsToAnnotation(
        annotation: FirAnnotation,
        signatureElements: List<SignatureElement>,
    ) {
        val pointsToValue = try {
            getPointsToValue(annotation, context.session, signatureElements.size)
        } catch (e: IllegalArgumentException) {
            reporter.reportOn(annotation.source, FirNativeErrors.INVALID_POINTS_TO_VALUE, e.message ?: "")
            return
        } ?: return

        for (indexFrom in signatureElements.indices) {
            val from = signatureElements[indexFrom]
            for (indexTo in signatureElements.indices) {
                val to = signatureElements[indexTo]
                val kind = try {
                    pointsToValue.kind(indexFrom, indexTo)
                } catch (e: IllegalArgumentException) {
                    reporter.reportOn(annotation.source, FirNativeErrors.INVALID_POINTS_TO_INDEX, indexFrom, indexTo, e.message ?: "")
                    null
                } ?: continue

                if (kind.sourceIsDirect && kind.destinationIsDirect) {
                    if (from.name != "<return>") {
                        reporter.reportOn(annotation.source, FirNativeErrors.POINTS_TO_KIND_1_ONLY_FOR_RETURN, from.name, to.name)
                    }
                }
                if (from.type.cannotEscape(context.session)) {
                    reporter.reportOn(annotation.source, FirNativeErrors.POINTS_TO_FROM_NON_ESCAPING_TYPE, from.name, to.name)
                    break
                }
                if (to.type.cannotEscape(context.session)) {
                    reporter.reportOn(annotation.source, FirNativeErrors.POINTS_TO_TO_NON_ESCAPING_TYPE, from.name, to.name)
                }
            }
        }
    }

    private fun getEscapesValue(annotation: FirAnnotation, session: FirSession, signatureSize: Int): Escapes? {
        val argument = annotation.findArgumentByName(StandardNames.NAME, returnFirstWhenNotFound = true) ?: return null
        val literal = argument.evaluateAs<FirLiteralExpression>(session) ?: return null
        val value = literal.value as? Int ?: return null
        return Escapes(value, signatureSize)
    }

    private fun getPointsToValue(annotation: FirAnnotation, session: FirSession, signatureSize: Int): PointsTo? {
        val argument = annotation.findArgumentByName(StandardNames.NAME, returnFirstWhenNotFound = true) ?: return null
        // PointsTo takes a vararg of integers
        val values = when (argument) {
            is FirVarargArgumentsExpression -> {
                argument.arguments.map { arg ->
                    val literal = arg.evaluateAs<FirLiteralExpression>(session) ?: return null
                    literal.value as? Int ?: return null
                }
            }
            else -> {
                // Single value case (shouldn't happen for valid PointsTo, but handle gracefully)
                val literal = argument.evaluateAs<FirLiteralExpression>(session) ?: return null
                val value = literal.value as? Int ?: return null
                listOf(value)
            }
        }
        return PointsTo(values, signatureSize)
    }

    private fun ConeKotlinType.cannotEscape(session: FirSession): Boolean {
        val type = this.fullyExpandedType(session)
        return type.isUnit || type.isNothing || FirInlineClassesSupport(session).computeBinaryType(type) is BinaryType.Primitive
    }

    private fun ConeKotlinType.mustEscape(session: FirSession): Boolean {
        val classSymbol = toRegularClassSymbol(session) ?: return false
        return classSymbol.getAnnotationByClassId(hasFinalizerClassId, session) != null
    }

    private data class SignatureElement(val name: String, val type: ConeKotlinType)

    // Helper functions
    private fun isPackageSupportedByEscapeAnalysis(packageFqName: FqName): Boolean =
        packageFqName.startsWith(kotlinPackageFqn) &&
                !packageFqName.startsWith(kotlinConcurrentFqn) &&
                !packageFqName.startsWith(kotlinNativeConcurrentFqn)

    context(context: CheckerContext)
    private fun isFunctionHandledByDFG(function: FirSimpleFunction): Boolean {
        val callableId = function.symbol.callableId
        return callableId.packageName == kotlinNativeInternalPackage &&
                function.name.asString() in symbolNamesHandledByDFG
    }

    context(context: CheckerContext)
    private fun isFunctionLoweredIntrinsic(function: FirSimpleFunction): Boolean {
        val intrinsicType = getIntrinsicType(function) ?: return false
        return intrinsicType in intrinsicsThatMustBeLowered
    }

    private fun collectSignatureElements(declaration: FirSimpleFunction): List<SignatureElement> = buildList {
        declaration.dispatchReceiverType?.let {
            add(SignatureElement("<this>", it))
        }
        declaration.contextParameters.forEach { contextParam ->
            add(SignatureElement(contextParam.name.asString(), contextParam.returnTypeRef.coneType))
        }
        declaration.receiverParameter?.let {
            add(SignatureElement("<receiver>", it.typeRef.coneType))
        }
        declaration.valueParameters.forEach {
            add(SignatureElement(it.name.asString(), it.returnTypeRef.coneType))
        }
        add(SignatureElement("<return>", declaration.returnTypeRef.coneType))
    }

    context(context: CheckerContext)
    private fun getIntrinsicType(function: FirSimpleFunction): IntrinsicType? {
        val annotation = function.getAnnotationByClassId(typedIntrinsicClassId, context.session) ?: return null
        val kindArgument = annotation.findArgumentByName(Name.identifier("kind"), returnFirstWhenNotFound = true) ?: return null
        val literal = kindArgument.evaluateAs<FirLiteralExpression>(context.session) ?: return null
        val kindString = literal.value as? String ?: return null

        return try {
            IntrinsicType.valueOf(kindString)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}