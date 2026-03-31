/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getConstructedClass
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dependencies.dependencyGraph
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.EnclosingEntity.Companion.asObjectEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.semantics.NodeIndex.Companion.beginIndex

object FirUninitializedAccessInObjectConstructorChecker : FirConstructorChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirConstructor) {
        if (!declaration.isPrimary) return
        declaration.symbol.getConstructedClass(context.session)?.let { constructedClass ->
            constructedClass.asObjectEntity()?.let { enclosingEntity ->
                val dependencyGraph = context.session.dependencyGraph
                val index = enclosingEntity.beginIndex()
                if (dependencyGraph.isBad(index)) {
                    dependencyGraph.badAccessesFor(index).forEach {
                        val declaration = when (it) {
                            is FirResolvedQualifier -> it.symbol!!
                            else -> it.toResolvedCallableSymbol(context.session)!!
                        }
                        reporter.reportOn(it.source, FirErrors.UNINITIALIZED_ACCESS, declaration)
                    }
                }
            }
        }
    }
}