/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isCompiledToJvmDefault
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isJvm6
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.java.jvmDefaultModeState
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.impl.FirClassUseSiteMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID

object FirJvmDefaultChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val jvmDefaultMode = context.session.jvmDefaultModeState
        val defaultAnnotation = declaration.getAnnotationByClassId(JVM_DEFAULT_CLASS_ID)

        if (defaultAnnotation != null) {
            val containingDeclaration = context.findClosest<FirClassLikeDeclaration>()
            if (containingDeclaration !is FirClass || !containingDeclaration.isInterface) {
                reporter.reportOn(defaultAnnotation.source, FirJvmErrors.JVM_DEFAULT_NOT_IN_INTERFACE, context)
                return
            } else if (context.isJvm6()) {
                reporter.reportOn(defaultAnnotation.source, FirJvmErrors.JVM_DEFAULT_IN_JVM6_TARGET, "JvmDefault", context)
                return
            } else if (!jvmDefaultMode.isEnabled) {
                reporter.reportOn(defaultAnnotation.source, FirJvmErrors.JVM_DEFAULT_IN_DECLARATION, "JvmDefault", context)
                return
            }
        } else {
            val annotation = declaration.getAnnotationByClassId(JVM_DEFAULT_NO_COMPATIBILITY_CLASS_ID)
            if (annotation != null) {
                if (context.isJvm6()) {
                    reporter.reportOn(
                        annotation.source,
                        FirJvmErrors.JVM_DEFAULT_IN_JVM6_TARGET,
                        "JvmDefaultWithoutCompatibility",
                        context
                    )
                    return
                } else if (!jvmDefaultMode.isEnabled) {
                    reporter.reportOn(
                        annotation.source,
                        FirJvmErrors.JVM_DEFAULT_IN_DECLARATION,
                        "JvmDefaultWithoutCompatibility",
                        context
                    )
                    return
                }
            }
        }

        if (declaration is FirClass) {
            val unsubstitutedScope = declaration.unsubstitutedScope(context)
            val hasDeclaredJvmDefaults = unsubstitutedScope is FirClassUseSiteMemberScope &&
                    unsubstitutedScope.directOverriddenFunctions.keys.any {
                        it.isCompiledToJvmDefault(jvmDefaultMode)
                    }
            if (!hasDeclaredJvmDefaults && !declaration.checkJvmDefaultsInHierarchy(jvmDefaultMode, context)) {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_DEFAULT_THROUGH_INHERITANCE, context)
            }
        }

        checkNonJvmDefaultOverridesJavaDefault(defaultAnnotation, jvmDefaultMode, declaration, context, reporter)
    }

    private fun FirDeclaration.checkJvmDefaultsInHierarchy(jvmDefaultMode: JvmDefaultMode, context: CheckerContext): Boolean {
        if (jvmDefaultMode.isEnabled) return true

        if (this !is FirClass) return true

        val unsubstitutedScope = unsubstitutedScope(context)
        if (unsubstitutedScope is FirClassUseSiteMemberScope) {
            val directOverriddenFunctions = unsubstitutedScope.directOverriddenFunctions.flatMap { it.value }.toSet()

            for (key in unsubstitutedScope.overrideByBase.keys) {
                if (directOverriddenFunctions.contains(key)) {
                    continue
                }

                if (key.getOverriddenDeclarations().all {
                        it.modality == Modality.ABSTRACT ||
                                !it.isCompiledToJvmDefaultWithProperMode(jvmDefaultMode) ||
                                it.containingClass()?.toFirRegularClassSymbol(context.session)?.isInterface != true
                    }
                ) {
                    continue
                }

                return false
            }
        }

        return true
    }

    private fun checkNonJvmDefaultOverridesJavaDefault(
        defaultAnnotation: FirAnnotation?,
        jvmDefaultMode: JvmDefaultMode,
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (defaultAnnotation != null || jvmDefaultMode.forAllMethodsWithBody) return
        val member = declaration as? FirSimpleFunction ?: return
        if (declaration is FirPropertyAccessor) return

        val containingDeclaration = context.findClosest<FirClassLikeDeclaration>()
        if (containingDeclaration is FirClass && containingDeclaration.isInterface) {
            val unsubstitutedScope = containingDeclaration.unsubstitutedScope(context)
            unsubstitutedScope.processFunctionsByName(member.name) {}
            val overriddenFunctions = unsubstitutedScope.getDirectOverriddenFunctions(member.symbol)

            if (overriddenFunctions.any { it.getAnnotationByClassId(JVM_DEFAULT_CLASS_ID) != null }) {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_DEFAULT_REQUIRED_FOR_OVERRIDE, context)
            } else if (jvmDefaultMode.isEnabled) {
                for (overriddenFunction in overriddenFunctions) {
                    val overriddenDeclarations = overriddenFunction.getOverriddenDeclarations()
                    for (overriddenDeclaration in overriddenDeclarations) {
                        val containingClassSymbol = overriddenDeclaration.containingClass()?.toSymbol(context.session)
                        if (containingClassSymbol?.origin is FirDeclarationOrigin.Java &&
                            overriddenDeclaration.modality != Modality.ABSTRACT
                        ) {
                            reporter.reportOn(declaration.source, FirJvmErrors.NON_JVM_DEFAULT_OVERRIDES_JAVA_DEFAULT, context)
                            return
                        }
                    }
                }
            }
        }
    }

    fun FirCallableSymbol<*>.getOverriddenDeclarations(): List<FirCallableSymbol<*>> {
        return if (this is FirIntersectionCallableSymbol) {
            ArrayList(this.intersections)
        } else {
            ArrayList<FirCallableSymbol<*>>(1).also { it.add(this) }
        }
    }

    fun FirCallableSymbol<*>.isCompiledToJvmDefaultWithProperMode(jvmDefaultMode: JvmDefaultMode): Boolean {
        // TODO: Fix support for all cases
        return isCompiledToJvmDefault(jvmDefaultMode)
    }
}
