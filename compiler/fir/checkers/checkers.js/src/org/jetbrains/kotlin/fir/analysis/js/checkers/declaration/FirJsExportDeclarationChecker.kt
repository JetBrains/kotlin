/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationFirstArgument
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.js.checkers.isExportedObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.sanitizeName
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.SPECIAL_KEYWORDS
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsExportDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isExportedObject(context) || declaration !is FirMemberDeclaration) {
            return
        }

        fun checkTypeParameter(typeParameter: FirTypeParameterRef) {
            for (upperBound in typeParameter.symbol.resolvedBounds) {
                if (!upperBound.type.isExportable(context.session)) {
                    reporter.reportOn(typeParameter.source, FirJsErrors.NON_EXPORTABLE_TYPE, "upper bound", upperBound.type, context)
                }
            }
        }

        fun checkValueParameter(valueParameter: FirValueParameter) {
            val type = valueParameter.returnTypeRef.coneType
            if (!type.isExportable(context.session)) {
                reporter.reportOn(valueParameter.source, FirJsErrors.NON_EXPORTABLE_TYPE, "parameter", type, context)
            }
        }

        val hasJsName = declaration.hasAnnotation(JsStandardClassIds.Annotations.JsName, context.session)

        fun reportWrongExportedDeclaration(kind: String) {
            reporter.reportOn(declaration.source, FirJsErrors.WRONG_EXPORTED_DECLARATION, kind, context)
        }

        if (declaration.isExpect) {
            reportWrongExportedDeclaration("expect")
        }

        validateDeclarationOnConsumableName(declaration, context, reporter)

        when (declaration) {
            is FirFunction -> {
                for (typeParameter in declaration.typeParameters) {
                    checkTypeParameter(typeParameter)
                }

                if (declaration.isInlineWithReified) {
                    reportWrongExportedDeclaration("inline function with reified type parameters")
                    return
                }

                if (declaration.isSuspend) {
                    reportWrongExportedDeclaration("suspend function")
                    return
                }

                if (declaration is FirConstructor && !declaration.isPrimary && !hasJsName) {
                    reportWrongExportedDeclaration("secondary constructor without @JsName")
                }

                // Properties are checked instead of property accessors
                if (declaration is FirPropertyAccessor) {
                    return
                }

                for (parameter in declaration.valueParameters) {
                    checkValueParameter(parameter)
                }

                val returnType = declaration.returnTypeRef.coneType

                if (!returnType.isExportable(context.session)) {
                    reporter.reportOn(declaration.source, FirJsErrors.NON_EXPORTABLE_TYPE, "return type", returnType, context)
                }
            }

            is FirProperty -> {
                if (declaration.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                    return
                }

                if (declaration.isExtension) {
                    reportWrongExportedDeclaration("extension property")
                    return
                }

                val returnType = declaration.returnTypeRef.coneType

                if (!returnType.isExportable(context.session)) {
                    reporter.reportOn(declaration.source, FirJsErrors.NON_EXPORTABLE_TYPE, "return type", returnType, context)
                }
            }

            is FirClass -> {
                for (typeParameter in declaration.typeParameters) {
                    checkTypeParameter(typeParameter)
                }

                val wrongDeclaration: String? = when (declaration.classKind) {
                    ClassKind.ANNOTATION_CLASS -> "annotation class"
                    ClassKind.CLASS -> when {
                        context.isInsideInterface -> "nested class inside exported interface"
                        declaration.isInline -> "value class"
                        else -> null
                    }
                    else -> if (context.isInsideInterface) {
                        "${if (declaration.status.isCompanion) "companion object" else "nested/inner declaration"} inside exported interface"
                    } else null
                }

                if (wrongDeclaration != null) {
                    reportWrongExportedDeclaration(wrongDeclaration)
                }
            }

            else -> {}
        }
    }

    private val CheckerContext.isInsideInterface
        get(): Boolean {
            val parent = containingDeclarations.lastOrNull() as? FirClass
            return parent != null && parent.isInterface
        }

    private val FirCallableDeclaration.isInlineWithReified: Boolean
        get() = when (this) {
            is FirPropertyAccessor -> {
                @OptIn(SymbolInternals::class)
                this.propertySymbol.fir.isInlineWithReified
            }
            else -> typeParameters.any { it.symbol.isReified }
        }

    private fun ConeKotlinType.isExportableReturn(session: FirSession, currentlyProcessed: MutableSet<ConeKotlinType> = mutableSetOf()) =
        isUnit || isExportable(session, currentlyProcessed)

    private fun ConeKotlinType.isExportable(
        session: FirSession,
        currentlyProcessed: MutableSet<ConeKotlinType> = mutableSetOf(),
    ): Boolean {
        if (!currentlyProcessed.add(this)) {
            return true
        }

        currentlyProcessed.add(this)
        val hasNonExportableArgument = typeArguments.any { it.type?.isExportable(session, currentlyProcessed) != true }

        if (hasNonExportableArgument) {
            currentlyProcessed.remove(this)
            return false
        }

        currentlyProcessed.remove(this)

        if (isBasicFunctionType(session)) {
            typeArguments.lastOrNull()?.type?.isExportableReturn(session, currentlyProcessed)
        }

        val nonNullable = withNullability(ConeNullability.NOT_NULL, session.typeContext)
        val isPrimitiveExportableType = nonNullable.isAny || nonNullable.isNullableAny
                || nonNullable is ConeDynamicType || isPrimitiveExportableConeKotlinType
        val symbol = toSymbol(session)

        return when {
            isPrimitiveExportableType -> true
            @OptIn(SymbolInternals::class)
            symbol?.fir is FirMemberDeclaration -> false
            isEnum -> true
            else -> symbol?.isEffectivelyExternal(session) == true || symbol?.isExportedObject(session) == true
        }
    }

    private val ConeKotlinType.isPrimitiveExportableConeKotlinType: Boolean
        get() = this is ConeTypeParameterType
                || isBoolean
                || isThrowableOrNullableThrowable
                || isString
                || isPrimitiveNumberOrNullableType && !isLong
                || isNothingOrNullableNothing
                || isArrayType

    private fun validateDeclarationOnConsumableName(
        declaration: FirMemberDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!context.isTopLevel || declaration.nameOrSpecialName.isSpecial) {
            return
        }

        val jsNameArgument = declaration.symbol.getAnnotationFirstArgument(JsStandardClassIds.Annotations.JsName, context.session)
        val reportTarget = jsNameArgument?.source ?: declaration.source
        val name = (jsNameArgument as? FirConstExpression<*>)?.value as? String ?: declaration.nameOrSpecialName.asString()

        if (name in SPECIAL_KEYWORDS || (name !in RESERVED_KEYWORDS && sanitizeName(name) == name)) {
            return
        }

        reporter.reportOn(reportTarget, FirJsErrors.NON_CONSUMABLE_EXPORTED_IDENTIFIER, name, context)
    }
}
