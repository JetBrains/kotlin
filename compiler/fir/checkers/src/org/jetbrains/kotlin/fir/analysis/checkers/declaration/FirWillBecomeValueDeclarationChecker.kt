/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.checkers.willBecomeValueInapplicableTarget
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.isMethodOfAny
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Checks that '@WillBecomeValue' is applied to a class which can actually become a value class, and that such a class
 * does not rely on the identity-based 'equals'/'hashCode'/'toString' inherited from 'Any'.
 *
 * The remaining value class declaration checks are shared with real value classes and are performed by
 * [FirValueClassDeclarationChecker].
 */
object FirWillBecomeValueDeclarationChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    private val identityBasedMemberNames = listOf(
        OperatorNameConventions.EQUALS,
        OperatorNameConventions.HASH_CODE,
        OperatorNameConventions.TO_STRING,
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val annotation = declaration.getAnnotationByClassId(StandardClassIds.Annotations.WillBecomeValue, context.session) ?: return

        val inapplicableTarget = declaration.symbol.willBecomeValueInapplicableTarget()
        if (inapplicableTarget != null) {
            reporter.reportOn(annotation.source, FirErrors.WILL_BECOME_VALUE_NOT_APPLICABLE, inapplicableTarget)
            return
        }

        checkIdentityBasedMembers(declaration)
    }

    /**
     * Once the class becomes a value class, its 'equals'/'hashCode'/'toString' become structural, so a class that still
     * inherits the identity-based implementations from 'Any' would silently change behavior. 'Any.toString' is
     * identity-based as well: it renders the identity hash code.
     *
     * Only final classes are checked. An abstract or sealed value class has no 'equals'/'hashCode' of its own; they come
     * from its concrete subclasses. And for an object there is a single instance, so identity comparison already
     * coincides with the structural one.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkIdentityBasedMembers(declaration: FirRegularClass) {
        if (declaration.classKind != ClassKind.CLASS || !declaration.isFinal) return

        val classScope = declaration.unsubstitutedScope()
        for (name in identityBasedMemberNames) {
            var isInheritedFromAny = false
            classScope.processFunctionsByName(name) {
                if (!it.isMethodOfAny) return@processFunctionsByName
                if (it.unwrapFakeOverrides().getContainingClassSymbol()?.classId == StandardClassIds.Any) {
                    isInheritedFromAny = true
                }
            }
            if (isInheritedFromAny) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS,
                    name.asString(),
                )
            }
        }
    }
}
