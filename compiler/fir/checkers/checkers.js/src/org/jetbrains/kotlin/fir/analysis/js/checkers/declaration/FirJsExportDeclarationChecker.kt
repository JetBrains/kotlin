/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationFirstArgument
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isExportedObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.sanitizeName
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.isEnumEntries
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.SPECIAL_KEYWORDS
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsExportDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.symbol.isExportedObject(context) || declaration !is FirMemberDeclaration) {
            return
        }

        fun checkTypeParameter(typeParameter: FirTypeParameterRef) {
            if (typeParameter is FirConstructedClassTypeParameterRef) {
                return
            }
            for (upperBound in typeParameter.symbol.resolvedBounds) {
                if (!upperBound.type.isExportable(context.session)) {
                    val source = upperBound.source ?: typeParameter.source ?: declaration.source
                    reporter.reportOn(source, FirJsErrors.NON_EXPORTABLE_TYPE, "upper bound", upperBound.type, context)
                }
            }
        }

        fun checkValueParameter(valueParameter: FirValueParameter) {
            val type = valueParameter.returnTypeRef.coneType
            if (!type.isExportable(context.session)) {
                val source = valueParameter.source ?: declaration.source
                reporter.reportOn(source, FirJsErrors.NON_EXPORTABLE_TYPE, "parameter", type, context)
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

                if (declaration.symbol.isInlineWithReified) {
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

                if (declaration !is FirConstructor && !returnType.isExportableReturn(context.session)) {
                    reporter.reportOn(declaration.source, FirJsErrors.NON_EXPORTABLE_TYPE, "return", returnType, context)
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

                val containingClass = declaration.getContainingClassSymbol(context.session) as? FirClassSymbol<*>
                val enumEntriesProperty = containingClass?.let(declaration::isEnumEntries) ?: false
                val returnType = declaration.returnTypeRef.coneType
                if (!enumEntriesProperty && !returnType.isExportable(context.session)) {
                    reporter.reportOn(declaration.source, FirJsErrors.NON_EXPORTABLE_TYPE, "property", returnType, context)
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

    private val FirCallableSymbol<*>.isInlineWithReified: Boolean
        get() = when (this) {
            is FirPropertyAccessorSymbol -> {
                this.propertySymbol.isInlineWithReified
            }
            else -> typeParameterSymbols.any { it.isReified }
        }

    private fun ConeKotlinType.isExportableReturn(session: FirSession, currentlyProcessed: MutableSet<ConeKotlinType> = hashSetOf()) =
        isUnit || isExportable(session, currentlyProcessed)

    private fun ConeKotlinType.isExportableTypeArguments(
        session: FirSession,
        currentlyProcessed: MutableSet<ConeKotlinType>,
        isFunctionType: Boolean
    ): Boolean {
        if (typeArguments.isEmpty()) {
            return true
        }
        for (i in 0 until typeArguments.lastIndex) {
            if (typeArguments[i].type?.isExportable(session, currentlyProcessed) != true) {
                return false
            }
        }
        val isLastExportable = if (isFunctionType) {
            typeArguments.last().type?.isExportableReturn(session, currentlyProcessed)
        } else {
            typeArguments.last().type?.isExportable(session, currentlyProcessed)
        }
        return isLastExportable == true
    }

    private fun ConeKotlinType.isExportable(
        session: FirSession,
        currentlyProcessed: MutableSet<ConeKotlinType> = hashSetOf(),
    ): Boolean {
        if (!currentlyProcessed.add(this)) {
            return true
        }

        val isFunctionType = isBasicFunctionType(session)
        val isExportableArgs = isExportableTypeArguments(session, currentlyProcessed, isFunctionType)
        currentlyProcessed.remove(this)
        if (isFunctionType || !isExportableArgs) {
            return isExportableArgs
        }

        val nonNullable = withNullability(ConeNullability.NOT_NULL, session.typeContext)
        val isPrimitiveExportableType = nonNullable.isAny || nonNullable.isNullableAny
                || nonNullable is ConeDynamicType || nonNullable.isPrimitiveExportableConeKotlinType
        val symbol = toSymbol(session)

        return when {
            isPrimitiveExportableType -> true
            symbol?.isMemberDeclaration != true -> false
            isEnum -> true
            else -> symbol.isEffectivelyExternal(session) || symbol.isExportedObject(session)
        }
    }

    private val ConeKotlinType.isPrimitiveExportableConeKotlinType: Boolean
        get() = this is ConeTypeParameterType
                || isBoolean
                || isThrowableOrNullableThrowable
                || isString
                || isPrimitiveNumberOrNullableType && !isLong
                || isNothingOrNullableNothing
                || isPrimitiveArray
                || isNonPrimitiveArray
                || isList
                || isMutableList
                || isSet
                || isMutableSet
                || isMap
                || isMutableMap

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
        val name = (jsNameArgument as? FirLiteralExpression<*>)?.value as? String ?: declaration.nameOrSpecialName.asString()

        if (name in SPECIAL_KEYWORDS || (name !in RESERVED_KEYWORDS && sanitizeName(name) == name)) {
            return
        }

        reporter.reportOn(reportTarget, FirJsErrors.NON_CONSUMABLE_EXPORTED_IDENTIFIER, name, context)
    }
}
