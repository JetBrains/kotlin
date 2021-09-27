/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

object FirAnnotationChecker : FirAnnotatedDeclarationChecker() {
    private val deprecatedClassId = FqName("kotlin.Deprecated")
    private val deprecatedSinceKotlinClassId = FqName("kotlin.DeprecatedSinceKotlin")

    override fun check(
        declaration: FirAnnotatedDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        var deprecated: FirAnnotation? = null
        var deprecatedSinceKotlin: FirAnnotation? = null

        for (annotation in declaration.annotations) {
            val fqName = annotation.fqName(context.session) ?: continue
            if (fqName == deprecatedClassId) {
                deprecated = annotation
            } else if (fqName == deprecatedSinceKotlinClassId) {
                deprecatedSinceKotlin = annotation
            }

            withSuppressedDiagnostics(annotation, context) {
                checkAnnotationTarget(declaration, annotation, context, reporter)
            }
        }
        if (deprecatedSinceKotlin != null) {
            withSuppressedDiagnostics(deprecatedSinceKotlin, context) {
                checkDeprecatedCalls(deprecatedSinceKotlin, deprecated, context, reporter)
            }
        }

        checkRepeatedAnnotations(declaration, context, reporter)

        if (declaration is FirProperty) {
            checkRepeatedAnnotationsInProperty(declaration, context, reporter)
        } else if (declaration is FirCallableDeclaration) {
            if (declaration.source?.kind !is FirFakeSourceElementKind) {
                checkRepeatedAnnotations(declaration.returnTypeRef.coneTypeSafe(), context, reporter)
            }
        } else if (declaration is FirTypeAlias) {
            checkRepeatedAnnotations(declaration.expandedTypeRef.coneType, context, reporter)
        }
    }

    private fun checkAnnotationTarget(
        declaration: FirAnnotatedDeclaration,
        annotation: FirAnnotation,
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
        annotated: FirAnnotatedDeclaration,
        annotation: FirAnnotation,
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
        deprecatedSinceKotlin: FirAnnotation,
        deprecated: FirAnnotation?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val closestFirFile = context.findClosest<FirFile>()
        if (closestFirFile != null && !closestFirFile.packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE.shortName())) {
            reporter.reportOn(
                deprecatedSinceKotlin.source,
                FirErrors.DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE,
                context
            )
        }

        if (deprecated == null) {
            reporter.reportOn(deprecatedSinceKotlin.source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED, context)
        } else {
            val argumentMapping = deprecated.argumentMapping.mapping
            for (name in argumentMapping.keys) {
                if (name.identifier == "level") {
                    reporter.reportOn(
                        deprecatedSinceKotlin.source,
                        FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL,
                        context
                    )
                    break
                }
            }
        }
    }

    private fun checkRepeatedAnnotations(
        annotationContainer: FirAnnotationContainer,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        checkRepeatedAnnotation(annotationContainer, annotationContainer.annotations, context, reporter)
    }

    private fun checkRepeatedAnnotations(
        type: ConeKotlinType?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (type == null) return
        val fullyExpandedType = type.fullyExpandedType(context.session)
        checkRepeatedAnnotation(null, fullyExpandedType.attributes.customAnnotations, context, reporter)
        for (typeArgument in fullyExpandedType.typeArguments) {
            if (typeArgument is ConeKotlinType) {
                checkRepeatedAnnotations(typeArgument, context, reporter)
            }
        }
    }

    private fun checkRepeatedAnnotationsInProperty(
        property: FirProperty,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun FirAnnotationContainer?.getAnnotationTypes(): List<ConeKotlinType> {
            return this?.annotations?.map { it.annotationTypeRef.coneType } ?: listOf()
        }

        val propertyAnnotations = mapOf(
            AnnotationUseSiteTarget.PROPERTY_GETTER to property.getter?.getAnnotationTypes(),
            AnnotationUseSiteTarget.PROPERTY_SETTER to property.setter?.getAnnotationTypes(),
            AnnotationUseSiteTarget.SETTER_PARAMETER to property.setter?.valueParameters?.single().getAnnotationTypes()
        )

        val isError = context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitRepeatedUseSiteTargetAnnotations)

        for (annotation in property.annotations) {
            val useSiteTarget = annotation.useSiteTarget ?: property.getDefaultUseSiteTarget(annotation, context)
            val existingAnnotations = propertyAnnotations[useSiteTarget] ?: continue

            if (annotation.annotationTypeRef.coneType in existingAnnotations && !annotation.isRepeatable(context.session)) {
                val factory = if (isError) FirErrors.REPEATED_ANNOTATION else FirErrors.REPEATED_ANNOTATION_WARNING
                if (annotation.source?.kind !is FirFakeSourceElementKind) {
                    reporter.reportOn(annotation.source, factory, context)
                }
            }
        }
    }
}

