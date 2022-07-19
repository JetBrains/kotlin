/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isChildOfSealedInlineClass
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isSealedInlineClass
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.supertypeAsSealedInlineClassType
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SEALED_INLINE_CHILD_OVERLAPPING_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_CLASS_ID

object FirJvmInlineApplicabilityChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation = declaration.getAnnotationByClassId(JVM_INLINE_ANNOTATION_CLASS_ID)
        if (annotation != null && !declaration.isInline) {
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS, context)
        } else if (annotation == null && declaration.isInline && !declaration.isExpect && declaration.classKind == ClassKind.CLASS &&
            !declaration.isChildOfSealedInlineClass(context.session)
        ) {
            reporter.reportOn(
                declaration.getModifier(KtTokens.VALUE_KEYWORD)?.source,
                FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION,
                context
            )
        }

        if (annotation != null && declaration.isChildOfSealedInlineClass(context.session)) {
            if (checkUnderlyingTypeIntersection(declaration, context.session)) {
                reporter.reportOn(declaration.source, SEALED_INLINE_CHILD_OVERLAPPING_TYPE, context)
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun checkUnderlyingTypeIntersection(firClass: FirRegularClass, session: FirSession): Boolean {
        val firstType =
            if (firClass.symbol.isSealedInlineClass()) session.builtinTypes.nullableAnyType.type
            else firClass.inlineClassRepresentation?.underlyingType ?: return false
        val first = firstType.toRegularClassSymbol(session) ?: return false
        val allChildren = firClass.supertypeAsSealedInlineClassType(session)?.toRegularClassSymbol(session)?.fir
            ?.getSealedClassInheritors(session) ?: return false
        for (childId in allChildren) {
            if (childId == firClass.classId) continue
            val child = session.symbolProvider.getClassLikeSymbolByClassId(childId) as? FirRegularClassSymbol ?: continue
            if (child.getAnnotationByClassId(JVM_INLINE_ANNOTATION_CLASS_ID) == null) continue
            val secondType =
                if (child.isSealedInlineClass()) session.builtinTypes.nullableAnyType.type
                else (child.fir.inlineClassRepresentation?.underlyingType ?: continue)

            val second = secondType.toRegularClassSymbol(session) ?: continue

            val intersects = if (first.isFinal && second.isFinal) {
                first.classId == second.classId
            } else if (first.isFinal || first.classKind == ClassKind.CLASS) {
                firstType.isSubtypeOf(secondType, session)
            } else {
                first.isInterface && second.isInterface
            }

            if (intersects) {
                return true
            }
        }

        return false
    }
}