/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.getAllowedAnnotationTargets
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.AnnotationTargetList
import org.jetbrains.kotlin.resolve.AnnotationTargetLists

object FirAnnotationChecker : FirAnnotatedDeclarationChecker() {
    private val deprecatedClassId = FqName("kotlin.Deprecated")
    private val deprecatedSinceKotlinClassId = FqName("kotlin.DeprecatedSinceKotlin")

    override fun check(
        declaration: FirAnnotatedDeclaration<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        var deprecatedCall: FirAnnotationCall? = null
        var deprecatedSinceKotlinCall: FirAnnotationCall? = null

        for (annotation in declaration.annotations) {
            val fqName = annotation.fqName(context.session)
            if (fqName == deprecatedClassId) {
                deprecatedCall = annotation
            } else if (fqName == deprecatedSinceKotlinClassId) {
                deprecatedSinceKotlinCall = annotation
            }
            withSuppressedDiagnostics(annotation, context) {
                checkAnnotationTarget(declaration, annotation, context, reporter)
            }
        }
        if (deprecatedSinceKotlinCall != null) {
            withSuppressedDiagnostics(deprecatedSinceKotlinCall, context) {
                checkDeprecatedCalls(deprecatedSinceKotlinCall, deprecatedCall, context, reporter)
            }
        }
    }

    private fun checkAnnotationTarget(
        declaration: FirAnnotatedDeclaration<*>,
        annotation: FirAnnotationCall,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (declaration is FirValueParameter && declaration.source?.hasValOrVar() == true) {
            // This will be checked later as property
            return
        }
        val actualTargets = getActualTargetList(declaration)
        val applicableTargets = annotation.getAllowedAnnotationTargets(context.session)
        val useSiteTarget = annotation.useSiteTarget

        fun check(targets: List<KotlinTarget>) = targets.any {
            it in applicableTargets && (useSiteTarget == null || KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it)
        }

        fun checkWithUseSiteTargets(): Boolean {
            if (useSiteTarget == null) return false

            val useSiteMapping = KotlinTarget.USE_SITE_MAPPING[useSiteTarget]
            return actualTargets.onlyWithUseSiteTarget.any { it in applicableTargets && it == useSiteMapping }
        }

        if (useSiteTarget != null) {
            checkAnnotationUseSiteTarget(declaration, annotation, useSiteTarget, context, reporter)
        }

        if (check(actualTargets.defaultTargets) || check(actualTargets.canBeSubstituted) || checkWithUseSiteTargets()) {
            return
        }

        val targetDescription = actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"
        if (useSiteTarget != null) {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                targetDescription,
                useSiteTarget.renderName,
                context
            )
        } else {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET,
                targetDescription,
                context
            )
        }
    }

    private fun checkAnnotationUseSiteTarget(
        annotated: FirAnnotatedDeclaration<*>,
        annotation: FirAnnotationCall,
        target: AnnotationUseSiteTarget,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        when (target) {
            AnnotationUseSiteTarget.PROPERTY,
            AnnotationUseSiteTarget.PROPERTY_GETTER -> {
            }
            AnnotationUseSiteTarget.FIELD -> {
                if (annotated is FirProperty && annotated.delegateFieldSymbol != null && !annotated.hasBackingField) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD, context)
                }
            }
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> {
                if (annotated is FirProperty && annotated.delegateFieldSymbol == null) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, context)
                }
            }
            AnnotationUseSiteTarget.PROPERTY_SETTER,
            AnnotationUseSiteTarget.SETTER_PARAMETER -> {
                if (annotated !is FirProperty || annotated.isLocal) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY, target.renderName, context)
                } else if (!annotated.isVar) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE, target.renderName, context)
                }
            }
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> when {
                annotated is FirValueParameter -> {
                    val container = context.containingDeclarations.lastOrNull()
                    if (container is FirConstructor && container.isPrimary) {
                        reporter.reportOn(annotation.source, FirErrors.REDUNDANT_ANNOTATION_TARGET, target.renderName, context)
                    } else {
                        reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET, context)
                    }
                }
                annotated is FirProperty && annotated.source?.kind == FirFakeSourceElementKind.PropertyFromParameter -> {
                }
                else -> reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET, context)
            }
            AnnotationUseSiteTarget.FILE -> {
                // NB: report once?
                if (annotated !is FirFile) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_FILE_TARGET, context)
                }
            }
            AnnotationUseSiteTarget.RECEIVER -> {
                // NB: report once?
                // annotation with use-site target `receiver` can be only on type reference, but not on declaration
                reporter.reportOn(
                    annotation.source, FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET, "declaration", target.renderName, context
                )
            }
        }
    }

    private fun checkDeprecatedCalls(
        deprecatedSinceKotlinCall: FirAnnotationCall,
        deprecatedCall: FirAnnotationCall?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val closestFirFile = context.findClosest<FirFile>()
        if (closestFirFile != null && !closestFirFile.packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE.shortName())) {
            reporter.reportOn(
                deprecatedSinceKotlinCall.source,
                FirErrors.DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE,
                context
            )
        }

        if (deprecatedCall == null) {
            reporter.reportOn(deprecatedSinceKotlinCall.source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED, context)
        } else {
            val argumentMapping = deprecatedCall.argumentMapping ?: return
            for (value in argumentMapping.values) {
                if (value.name.identifier == "level") {
                    reporter.reportOn(
                        deprecatedSinceKotlinCall.source,
                        FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL,
                        context
                    )
                    break
                }
            }
        }
    }

    private fun getActualTargetList(annotated: FirDeclaration<*>): AnnotationTargetList {
        return when (annotated) {
            is FirRegularClass -> {
                AnnotationTargetList(
                    KotlinTarget.classActualTargets(annotated.classKind, annotated.isInner, annotated.isCompanion, annotated.isLocal)
                )
            }
            is KtDestructuringDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
            is FirProperty -> {
                when {
                    annotated.isLocal ->
                        if (annotated.source?.kind == FirFakeSourceElementKind.DesugaredComponentFunctionCall) {
                            TargetLists.T_DESTRUCTURING_DECLARATION
                        } else {
                            TargetLists.T_LOCAL_VARIABLE
                        }
                    annotated.symbol.callableId.classId != null ->
                        if (annotated.source?.kind == FirFakeSourceElementKind.PropertyFromParameter) {
                            TargetLists.T_VALUE_PARAMETER_WITH_VAL
                        } else {
                            TargetLists.T_MEMBER_PROPERTY(annotated.hasBackingField, annotated.delegate != null)
                        }
                    else ->
                        TargetLists.T_TOP_LEVEL_PROPERTY(annotated.hasBackingField, annotated.delegate != null)
                }
            }
            is FirValueParameter -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
            is FirConstructor -> TargetLists.T_CONSTRUCTOR
            is FirAnonymousFunction -> {
                TargetLists.T_FUNCTION_EXPRESSION
            }
            is FirSimpleFunction -> {
                when {
                    annotated.isLocal -> TargetLists.T_LOCAL_FUNCTION
                    annotated.symbol.callableId.classId != null -> TargetLists.T_MEMBER_FUNCTION
                    else -> TargetLists.T_TOP_LEVEL_FUNCTION
                }
            }
            is FirTypeAlias -> TargetLists.T_TYPEALIAS
            is FirPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
            is FirFile -> TargetLists.T_FILE
            is FirTypeParameter -> TargetLists.T_TYPE_PARAMETER
            is FirAnonymousInitializer -> TargetLists.T_INITIALIZER
            is KtDestructuringDeclaration -> TargetLists.T_DESTRUCTURING_DECLARATION
            is KtLambdaExpression -> TargetLists.T_FUNCTION_LITERAL
            is FirAnonymousObject ->
                if (annotated.source?.kind == FirFakeSourceElementKind.EnumInitializer) {
                    AnnotationTargetList(
                        KotlinTarget.classActualTargets(
                            ClassKind.ENUM_ENTRY,
                            isInnerClass = false,
                            isCompanionObject = false,
                            isLocalClass = false
                        )
                    )
                } else {
                    TargetLists.T_OBJECT_LITERAL
                }
            else -> TargetLists.EMPTY
        }
    }
}

private typealias TargetLists = AnnotationTargetLists
