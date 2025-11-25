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
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.hasError
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ReturnValueStatus

abstract class FirMustUseReturnValueStatusComponent : FirSessionComponent {
    abstract fun computeMustUseReturnValueForCallable(
        session: FirSession,
        declaration: FirCallableSymbol<*>,
        isLocal: Boolean,
        containingClass: FirClassLikeSymbol<*>?,
        containingProperty: FirPropertySymbol?,
        overriddenStatuses: List<FirResolvedDeclarationStatus>,
    ): ReturnValueStatus

    abstract fun computeMustUseReturnValueForJavaCallable(
        session: FirSession,
        declaration: FirCallableSymbol<*>,
        containingClass: FirClassLikeSymbol<*>?,
        javaPackageAnnotations: List<ClassId>? = null,
    ): ReturnValueStatus

    open fun isExpectActualIgnorabilityCompatible(
        session: FirSession,
        expect: FirCallableSymbol<*>,
        actual: FirCallableSymbol<*>,
        containingExpectClass: FirRegularClassSymbol?,
    ): Boolean = true

    // FIXME (KTI-2545): One can't simply write errorprone package name, because whole com.google. package is relocated in kotlin-compiler-embeddable.
    // For the time being, string literal should be split.
    internal val errorPronePackageFqName: FqName = FqName.fromSegments(listOf("com", "google", "errorprone", "annotations"))

    private val ignorableReturnValueLikeAnnotations: Set<ClassId> = setOf(
        StandardClassIds.Annotations.IgnorableReturnValue,
        ClassId(errorPronePackageFqName, Name.identifier("CanIgnoreReturnValue")),
        // Apparently, org.jetbrains.annotations and org.springframework.lang do not have CanIgnoreReturnValue because they have slightly different design
    )

    fun hasIgnorableLikeAnnotation(list: List<ClassId>?): Boolean = list.orEmpty().any { it in ignorableReturnValueLikeAnnotations }

    private val JAVA_LANG_VOID = ClassId.topLevel(FqName("java.lang.Void"))

    fun isIgnorableType(type: ConeKotlinType): Boolean {
        if (type is ConeErrorType || type.hasError()) return true
        val classId = type.classId ?: return false
        if (classId == StandardClassIds.Nothing) return true
        if (classId == StandardClassIds.Unit && !type.isMarkedNullable) return true
        if (classId == JAVA_LANG_VOID && !type.isMarkedNullable) return true // Void? is not ignorable just as Unit?
        return false
    }

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
            overriddenStatuses: List<FirResolvedDeclarationStatus>,
        ): ReturnValueStatus {
            return overriddenStatuses.firstOrNull()?.returnValueStatus ?: ReturnValueStatus.Unspecified
        }

        override fun computeMustUseReturnValueForJavaCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            containingClass: FirClassLikeSymbol<*>?,
            javaPackageAnnotations: List<ClassId>?,
        ): ReturnValueStatus {
            return ReturnValueStatus.Unspecified
        }
    }

    private class Default : FirMustUseReturnValueStatusComponent() {
        private val mustUseReturnValueLikeAnnotations: Set<ClassId> = setOf(
            StandardClassIds.Annotations.MustUseReturnValues,
            ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("MustUseReturnValue")), // Pre-2.3.0 name, can be deleted later.
            ClassId(errorPronePackageFqName, Name.identifier("CheckReturnValue")),
            ClassId(FqName("org.jetbrains.annotations"), Name.identifier("CheckReturnValue")),
            ClassId(FqName("org.springframework.lang"), Name.identifier("CheckReturnValue")),
            ClassId(FqName("org.jooq"), Name.identifier("CheckReturnValue")),
            ClassId(FqName("edu.umd.cs.findbugs.annotations"), Name.identifier("CheckReturnValue")),
        )

        private fun List<ClassId>?.hasMustUseReturnValueLikeAnnotation() = this.orEmpty().any { it in mustUseReturnValueLikeAnnotations }

        override fun computeMustUseReturnValueForJavaCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            containingClass: FirClassLikeSymbol<*>?,
            javaPackageAnnotations: List<ClassId>?,
        ): ReturnValueStatus {
            if (hasIgnorableLikeAnnotation(declaration.resolvedAnnotationClassIds)) return ReturnValueStatus.ExplicitlyIgnorable

            if (findMustUseAmongContainers(session, declaration, containingClass, containingProperty = null, additionalAnnotations = javaPackageAnnotations))
                return ReturnValueStatus.MustUse
            return ReturnValueStatus.Unspecified
        }

        override fun computeMustUseReturnValueForCallable(
            session: FirSession,
            declaration: FirCallableSymbol<*>,
            isLocal: Boolean,
            containingClass: FirClassLikeSymbol<*>?,
            containingProperty: FirPropertySymbol?,
            overriddenStatuses: List<FirResolvedDeclarationStatus>,
        ): ReturnValueStatus {
            val analysisMode = session.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode)
            if (isLocal) {
                // To compute status using annotations, getFirCallableContainerFile/getContainingDeclaration should work correctly for local declarations (KT-80564)
                return if (declaration is FirFunctionSymbol && analysisMode == ReturnValueCheckerMode.FULL) ReturnValueStatus.MustUse else ReturnValueStatus.Unspecified
            }
            // Implementation note: just with intersection overrides, in case we have more than one immediate parent, we take first from the list
            // See inheritanceChainIgnorability.kt test.
            val overriddenFlag = overriddenStatuses.firstOrNull()?.returnValueStatus

            if (hasIgnorableLikeAnnotation(declaration.resolvedAnnotationClassIds)) return ReturnValueStatus.ExplicitlyIgnorable
            if (overriddenFlag == ReturnValueStatus.MustUse) return ReturnValueStatus.MustUse

            // In the case of inheriting from Ignorable or Unspecified, global FULL setting has lesser priority than annotations/parent
            // but we want to check it here first to avoid looking through the containers
            val overridesIgnorableOrUnspecified = overriddenFlag == ReturnValueStatus.ExplicitlyIgnorable || overriddenFlag == ReturnValueStatus.Unspecified
            if (analysisMode == ReturnValueCheckerMode.FULL && !overridesIgnorableOrUnspecified)
                return ReturnValueStatus.MustUse

            if (findMustUseAmongContainers(
                    session = session,
                    declaration = declaration,
                    containingClass = containingClass,
                    containingProperty = containingProperty,
                    additionalAnnotations = null,
                )
            ) return ReturnValueStatus.MustUse

            // In case no annotations are provided, we inherit status from the parent.
            return overriddenFlag ?: ReturnValueStatus.Unspecified

        }

        override fun isExpectActualIgnorabilityCompatible(
            session: FirSession,
            expect: FirCallableSymbol<*>,
            actual: FirCallableSymbol<*>,
            containingExpectClass: FirRegularClassSymbol?,
        ): Boolean {
            if (isIgnorableType(expect.resolvedReturnType) || isIgnorableType(actual.resolvedReturnType)) return true
            val relaxedRules = actual.isNotDirectMember(containingExpectClass, session) || expect.isNotDirectMember(containingExpectClass, session)
            val expectStatus = expect.resolvedStatus.returnValueStatus
            val actualStatus = actual.resolvedStatus.returnValueStatus
            if (relaxedRules && (expectStatus == ReturnValueStatus.Unspecified || actualStatus == ReturnValueStatus.Unspecified)) return true
            return when (expectStatus) {
                ReturnValueStatus.MustUse -> actualStatus == ReturnValueStatus.MustUse
                ReturnValueStatus.ExplicitlyIgnorable, ReturnValueStatus.Unspecified -> actualStatus != ReturnValueStatus.MustUse
            }
        }

        /**
         * Same as FirExpectActualMatchingContextImpl.isFakeOverride, but also is able to handle constructors
         */
        fun FirCallableSymbol<*>.isNotDirectMember(containingExpectClass: FirRegularClassSymbol?, session: FirSession): Boolean {
            if (containingExpectClass == null) {
                return false
            }
            return when (this) {
                is FirConstructorSymbol -> this.getConstructedClass(session)?.classId != containingExpectClass.classId
                else if (dispatchReceiverType?.classId != containingExpectClass.classId) -> true
                else -> isSubstitutionOrIntersectionOverride
            }
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
