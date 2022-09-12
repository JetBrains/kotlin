/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@OptIn(SymbolInternals::class)
object FirJsRuntimeAnnotationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        for (annotation in declaration.annotations) {
            val annotationClass = annotation.coneClassLikeType?.lookupTag?.toSymbol(context.session)?.fir ?: continue
            if (annotationClass.getAnnotationRetention() != KotlinRetention.RUNTIME) continue

            if (declaration is FirMemberDeclaration && declaration.isEffectivelyExternal(context)) {
                reporter.reportOn(annotation.source, FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION, context)
            } else {
                reporter.reportOn(annotation.source, FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED, context)
            }
        }
    }

    private fun FirAnnotationContainer.getAnnotationRetention(): KotlinRetention? {
        return annotations.find { it.classId == StandardClassIds.Annotations.Retention }?.getAnnotationRetention()
    }

    private fun FirAnnotation.getAnnotationRetention(): KotlinRetention? {
        val callableId = argumentMapping.mapping[StandardClassIds.Annotations.ParameterNames.value]
            ?.safeAs<FirPropertyAccessExpression>()
            ?.calleeReference?.resolvedSymbol
            ?.safeAs<FirEnumEntrySymbol>()
            ?.callableId ?: return null

        if (callableId.classId != StandardClassIds.AnnotationRetention) {
            return null
        }

        return KotlinRetention.values().firstOrNull { it.name == callableId.callableName.asString() }
    }
}