/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.KOTLIN_THROWS_ANNOTATION_FQ_NAME

object FirNativeThrowsChecker : FirBasicDeclarationChecker() {
    private val throwsClassId = ClassId.topLevel(KOTLIN_THROWS_ANNOTATION_FQ_NAME)

    private val cancellationExceptionFqName = FqName("kotlin.coroutines.cancellation.CancellationException")

    private val cancellationExceptionAndSupersClassIds = setOf(
        ClassId.topLevel(StandardNames.FqNames.throwable),
        ClassId.topLevel(FqName("kotlin.Exception")),
        ClassId.topLevel(FqName("kotlin.RuntimeException")),
        ClassId.topLevel(FqName("kotlin.IllegalStateException")),
        ClassId.topLevel(cancellationExceptionFqName)
    )

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val throwsAnnotation = declaration.getAnnotationByClassId(throwsClassId) as? FirAnnotationCall

        if (!checkInheritance(declaration, throwsAnnotation, context, reporter)) return

        if (throwsAnnotation.hasUnresolvedArgument()) return

        val classTypes = throwsAnnotation?.getClassTypes(context.session) ?: return

        if (classTypes.isEmpty()) {
            reporter.reportOn(throwsAnnotation.source, FirNativeErrors.THROWS_LIST_EMPTY, context)
            return
        }

        if (declaration.hasModifier(KtTokens.SUSPEND_KEYWORD) && classTypes.none { it.classId in cancellationExceptionAndSupersClassIds }) {
            reporter.reportOn(
                throwsAnnotation.source,
                FirNativeErrors.MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND,
                cancellationExceptionFqName,
                context
            )
        }
    }

    private fun checkInheritance(
        declaration: FirDeclaration,
        throwsAnnotation: FirAnnotationCall?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        if (declaration !is FirSimpleFunction) return true

        val inherited = getInheritedThrows(declaration, throwsAnnotation, context).entries.distinctBy { it.value }

        if (inherited.size >= 2) {
            reporter.reportOn(
                declaration.source,
                FirNativeErrors.INCOMPATIBLE_THROWS_INHERITED,
                inherited.mapNotNull { it.key.containingClassLookupTag()?.toFirRegularClassSymbol(context.session) },
                context
            )
            return false
        }

        val (overriddenMember, overriddenThrows) = inherited.firstOrNull()
            ?: return true // Should not happen though.

        if (decodeThrowsFilter(throwsAnnotation, context.session) != overriddenThrows) {
            val containingClassSymbol = overriddenMember.containingClassLookupTag()?.toFirRegularClassSymbol(context.session)
            if (containingClassSymbol != null) {
                reporter.reportOn(throwsAnnotation?.source, FirNativeErrors.INCOMPATIBLE_THROWS_OVERRIDE, containingClassSymbol, context)
            }
            return false
        }

        return true
    }

    private fun getInheritedThrows(
        function: FirSimpleFunction,
        throwsAnnotation: FirAnnotationCall?,
        context: CheckerContext
    ): Map<FirNamedFunctionSymbol, ThrowsFilter> {
        val visited = mutableSetOf<FirNamedFunctionSymbol>()
        val result = mutableMapOf<FirNamedFunctionSymbol, ThrowsFilter>()

        fun getInheritedThrows(localThrowsAnnotation: FirAnnotationCall?, localFunctionSymbol: FirNamedFunctionSymbol) {
            if (!visited.add(localFunctionSymbol)) return
            val containingClassSymbol = localFunctionSymbol.containingClassLookupTag()?.toFirRegularClassSymbol(context.session)

            if (containingClassSymbol != null) {
                val unsubstitutedScope = containingClassSymbol.unsubstitutedScope(context)
                unsubstitutedScope.processFunctionsByName(localFunctionSymbol.name) {}
                val overriddenFunctions = unsubstitutedScope.getDirectOverriddenFunctions(localFunctionSymbol)
                if (localFunctionSymbol == function.symbol || localThrowsAnnotation == null && overriddenFunctions.isNotEmpty()) {
                    for (overriddenFunction in overriddenFunctions) {
                        val annotation = if (overriddenFunction.isSubstitutionOrIntersectionOverride) {
                            null
                        } else {
                            overriddenFunction.getAnnotationByClassId(throwsClassId) as? FirAnnotationCall
                        }
                        getInheritedThrows(annotation, overriddenFunction)
                    }
                } else {
                    result[localFunctionSymbol] = decodeThrowsFilter(localThrowsAnnotation, context.session)
                }
            }
        }

        getInheritedThrows(throwsAnnotation, function.symbol)

        return result
    }

    private fun FirElement?.hasUnresolvedArgument(): Boolean {
        if (this is FirWrappedArgumentExpression) {
            return expression.hasUnresolvedArgument()
        }

        if (this is FirResolvable) {
            if (this.calleeReference is FirErrorNamedReference) {
                return true
            }
        }

        if (this is FirVarargArgumentsExpression) {
            for (argument in this.arguments) {
                if (argument.hasUnresolvedArgument()) {
                    return true
                }
            }
        }

        if (this is FirCall) {
            for (argument in this.argumentList.arguments) {
                if (argument.hasUnresolvedArgument()) {
                    return true
                }
            }
        }

        return false
    }

    private fun decodeThrowsFilter(throwsAnnotation: FirAnnotationCall?, session: FirSession): ThrowsFilter {
        return ThrowsFilter(throwsAnnotation?.getClassTypes(session)?.toSet())
    }

    private fun FirAnnotationCall.getClassTypes(session: FirSession): List<ConeKotlinType> {
        val arguments = argumentList.arguments
        return (arguments.firstOrNull() as? FirVarargArgumentsExpression)?.arguments
            ?.filterIsInstance<FirGetClassCall>()
            ?.map { it.arguments.first().typeRef }
            ?.filterIsInstance<FirResolvedTypeRef>()
            ?.map { it.type.fullyExpandedType(session) }
            ?: emptyList()
    }

    private data class ThrowsFilter(val classes: Set<ConeKotlinType>?)
}
