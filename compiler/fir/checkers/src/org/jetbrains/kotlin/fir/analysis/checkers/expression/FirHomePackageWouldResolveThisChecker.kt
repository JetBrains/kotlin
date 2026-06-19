/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.hasResolvedType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirHomePackageWouldResolveThisChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    val defaultImports: Set<FqName> = setOf(
        FqName.fromSegments(listOf("kotlin")),
        FqName.fromSegments(listOf("kotlin", "annotation")),
        FqName.fromSegments(listOf("kotlin", "collections")),
        FqName.fromSegments(listOf("kotlin", "ranges")),
        FqName.fromSegments(listOf("kotlin", "sequences")),
        FqName.fromSegments(listOf("kotlin", "text")),
        FqName.fromSegments(listOf("kotlin", "io")),
        FqName.fromSegments(listOf("kotlin", "comparisons")),
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (LanguageFeature.HomePackageResolution.isDisabled()) return
        val receiver = expression.explicitReceiver ?: return
        if (!receiver.hasResolvedType) return
        val calleeSymbol = expression.calleeReference.toResolvedCallableSymbol() ?: return

        val receivers = receiver.resolvedType.allSuperTypes()
        val packages = receivers.map { it.classId.packageFqName } - defaultImports - listOfNotNull(context.containingFileSymbol?.packageFqName )
        val scope = FirExtensionsScope(receivers, packages.map { FirPackageMemberScope(it, context.session) }, context.session)

        var found = false
        scope.processFunctionsByName(calleeSymbol.name) { if (it == calleeSymbol) found = true }
        scope.processPropertiesByName(calleeSymbol.name) { if (it == calleeSymbol) found = true }

        if (found) {
            reporter.reportOn(expression.calleeReference.source, FirErrors.HOME_PACKAGE_WOULD_RESOLVE_THIS)
        }
    }

    context(context: CheckerContext)
    fun ConeKotlinType.allSuperTypes(to: MutableSet<FirClassSymbol<*>> = mutableSetOf()): Set<FirClassSymbol<*>> {
        val regularClass = this.toClassSymbol() ?: return to
        to += regularClass
        regularClass.resolvedSuperTypes.forEach { it.allSuperTypes(to) }
        return to
    }

    class FirExtensionsScope(
        private val receivers: Set<FirClassSymbol<*>>,
        private val underlying: FirScope,
        private val session: FirSession,
    ) : FirScope() {
        constructor(receivers: Set<FirClassSymbol<*>>, underlyingScopes: List<FirScope>, session: FirSession) :
                this(receivers, FirCompositeScope(underlyingScopes), session)

        @DelicateScopeAPI
        override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirScope? =
            FirExtensionsScope(receivers, underlying.withReplacedSessionOrNull(newSession, newScopeSession) ?: return null, newSession)

        override fun mayContainName(name: Name): Boolean = underlying.mayContainName(name)

        override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit): Unit =
            underlying.processFunctionsByName(name) { function ->
                val receiver = function.resolvedReceiverType?.toClassSymbol(session)
                if (receiver != null && receiver in receivers) processor(function)
            }

        override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit): Unit =
            underlying.processPropertiesByName(name) { property ->
                val receiver = property.resolvedReceiverType?.toClassSymbol(session)
                if (receiver != null && receiver in receivers) processor(property)
            }
    }
}
