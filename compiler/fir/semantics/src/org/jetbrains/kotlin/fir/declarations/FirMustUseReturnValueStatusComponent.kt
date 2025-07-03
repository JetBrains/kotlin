/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

abstract class FirMustUseReturnValueStatusComponent : FirSessionComponent {
    abstract fun computeMustUseReturnValueForCallable(
        session: FirSession,
        declaration: FirCallableSymbol<*>,
        isLocal: Boolean,
        containingClass: FirClassLikeSymbol<*>?,
        containingProperty: FirPropertySymbol?,
    ): Boolean

    abstract fun computeMustUseReturnValueForJavaCallable(
        session: FirSession,
        declaration: FirCallableSymbol<*>,
        containingClass: FirClassLikeSymbol<*>?,
        javaPackageAnnotations: List<ClassId>? = null,
    ): Boolean

    private val ignorableReturnValueLikeAnnotations: Set<ClassId> = setOf(
        StandardClassIds.Annotations.IgnorableReturnValue,
        ClassId(FqName("com.google.errorprone.annotations"), Name.identifier("CanIgnoreReturnValue")),
        // Apparently, org.jetbrains.annotations and org.springframework.lang do not have CanIgnoreReturnValue because they have slightly different design
    )

    fun hasIgnorableLikeAnnotation(list: List<ClassId>?): Boolean = list.orEmpty().any { it in ignorableReturnValueLikeAnnotations }

    companion object {
        fun create(languageVersionSettings: LanguageVersionSettings): FirMustUseReturnValueStatusComponent {
            return if (languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) == ReturnValueCheckerMode.DISABLED) Disabled
            else Default()
        }
    }

    private object Disabled : FirMustUseReturnValueStatusComponent() {
        override fun computeMustUseReturnValueForCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            isLocal: Boolean,
            containingClass: FirClassLikeSymbol<*>?,
            containingProperty: FirPropertySymbol?,
        ): Boolean {
            return false
        }

        override fun computeMustUseReturnValueForJavaCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            containingClass: FirClassLikeSymbol<*>?,
            javaPackageAnnotations: List<ClassId>?,
        ): Boolean {
            return false
        }
    }

    private class Default : FirMustUseReturnValueStatusComponent() {
        private val mustUseReturnValueLikeAnnotations: Set<ClassId> = setOf(
            StandardClassIds.Annotations.MustUseReturnValue,
            ClassId(FqName("com.google.errorprone.annotations"), Name.identifier("CheckReturnValue")),
            ClassId(FqName("org.jetbrains.annotations"), Name.identifier("CheckReturnValue")),
            ClassId(FqName("org.springframework.lang"), Name.identifier("CheckReturnValue")),
        )

        private fun List<ClassId>?.hasMustUseReturnValueLikeAnnotation() = this.orEmpty().any { it in mustUseReturnValueLikeAnnotations }

        override fun computeMustUseReturnValueForJavaCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            containingClass: FirClassLikeSymbol<*>?,
            javaPackageAnnotations: List<ClassId>?,
        ): Boolean {
            if (hasIgnorableLikeAnnotation(declaration.resolvedAnnotationClassIds)) return false

            return findMustUseAmongContainers(
                session, declaration, containingClass,
                containingProperty = null,
                additionalAnnotations = javaPackageAnnotations
            )
        }

        override fun computeMustUseReturnValueForCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            isLocal: Boolean,
            containingClass: FirClassLikeSymbol<*>?,
            containingProperty: FirPropertySymbol?
        ): Boolean {
            if (isLocal) {
                // FIXME (KT-78112): pass through outer declaration through BodyResolveTransformer when we compute status for local functions
                return declaration is FirFunctionSymbol
            }

            if (hasIgnorableLikeAnnotation(declaration.resolvedAnnotationClassIds)) return false

            if (session.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) == ReturnValueCheckerMode.FULL) return true

            return findMustUseAmongContainers(session, declaration, containingClass, containingProperty, additionalAnnotations = null)
        }

        private fun findMustUseAmongContainers(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            containingClass: FirClassLikeSymbol<*>?,
            containingProperty: FirPropertySymbol?,
            additionalAnnotations: List<ClassId>?,
        ): Boolean {
            // Checking the most probable places for annotation one-by-one to avoid computing unnecessary empty annotations lists:
            if (declaration.resolvedAnnotationClassIds.hasMustUseReturnValueLikeAnnotation()) return true
            if (containingClass?.resolvedAnnotationClassIds.hasMustUseReturnValueLikeAnnotation()) return true
            if (session.firProvider.getFirCallableContainerFile(declaration)?.symbol?.resolvedAnnotationClassIds.hasMustUseReturnValueLikeAnnotation()) return true
            if (containingProperty?.resolvedAnnotationClassIds.hasMustUseReturnValueLikeAnnotation()) return true
            if (additionalAnnotations.hasMustUseReturnValueLikeAnnotation()) return true
            // Outer classes:
            tailrec fun FirClassLikeSymbol<*>.hasMurvOrOuter(): Boolean {
                if (this.resolvedAnnotationClassIds.hasMustUseReturnValueLikeAnnotation()) return true
                val outer = this.getContainingDeclaration(session) ?: return false
                return outer.hasMurvOrOuter()
            }
            return containingClass?.getContainingDeclaration(session)?.hasMurvOrOuter() ?: false
        }
    }
}

val FirSession.mustUseReturnValueStatusComponent: FirMustUseReturnValueStatusComponent by FirSession.sessionComponentAccessor()
