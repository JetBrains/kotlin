/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.hasValOrVar
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.forEachExpandedType
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.AnnotationTargetListForDeprecation
import org.jetbrains.kotlin.resolve.AnnotationTargetLists
import org.jetbrains.kotlin.resolve.checkers.OptInNames.OPT_IN_CLASS_ID
import org.jetbrains.kotlin.utils.keysToMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(AnnotationTargetListForDeprecation::class)
object FirAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val deprecatedClassId = FqName("kotlin.Deprecated")
    private val deprecatedSinceKotlinClassId = FqName("kotlin.DeprecatedSinceKotlin")

    override fun check(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (declaration is FirDanglingModifierList) {
            return
        }

        checkAnnotationContainer(declaration, context, reporter)

        if (declaration is FirCallableDeclaration) {
            declaration.receiverParameter?.let {
                checkAnnotationContainer(it, context, reporter)
            }
        }
    }

    private fun checkAnnotationContainer(
        declaration: FirAnnotationContainer,
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

            checkAnnotationTarget(declaration, annotation, context, reporter)
        }

        if (declaration is FirCallableDeclaration) {
            val receiverParameter = declaration.receiverParameter
            if (receiverParameter != null) {
                for (receiverAnnotation in receiverParameter.annotations) {
                    reportIfMfvc(context, reporter, receiverAnnotation, "receivers", receiverParameter.typeRef.coneType)
                }
            }
        }

        if (deprecatedSinceKotlin != null) {
            checkDeprecatedCalls(deprecatedSinceKotlin, deprecated, context, reporter)
        }

        checkDeclaredRepeatedAnnotations(declaration, context, reporter)

        if (declaration is FirCallableDeclaration) {
            if (declaration is FirProperty) {
                checkRepeatedAnnotationsInProperty(declaration, context, reporter)
            }
            if (declaration is FirValueParameter) {
                checkPossibleMigrationToPropertyOrField(declaration, context, reporter)
            }

            if (declaration.source?.kind is KtRealSourceElementKind && declaration.returnTypeRef.source?.kind is KtRealSourceElementKind) {
                checkAllRepeatedAnnotations(declaration.returnTypeRef, context, reporter)
            }
        } else if (declaration is FirTypeAlias) {
            checkAllRepeatedAnnotations(declaration.expandedTypeRef, context, reporter)
        }
    }

    private fun reportIfMfvc(
        context: CheckerContext,
        reporter: DiagnosticReporter,
        annotation: FirAnnotation,
        hint: String,
        type: ConeKotlinType,
    ) {
        if (type.needsMultiFieldValueClassFlattening(context.session)) {
            reporter.reportOn(annotation.source, FirErrors.ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET, hint, context)
        }
    }

    private fun checkMultiFieldValueClassAnnotationRestrictions(
        declaration: FirAnnotationContainer,
        annotation: FirAnnotation,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        fun FirPropertyAccessor.hasNoReceivers() = contextParameters.isEmpty() && receiverParameter?.typeRef == null &&
                propertySymbol.resolvedReceiverTypeRef == null && propertySymbol.resolvedContextParameters.isEmpty()

        val (hint, type) = when (annotation.useSiteTarget) {
            FIELD -> "fields" to ((declaration as? FirBackingField)?.returnTypeRef?.coneType ?: return)
            PROPERTY_DELEGATE_FIELD -> "delegate fields" to ((declaration as? FirBackingField)?.propertySymbol?.delegate?.resolvedType
                ?: return)
            RECEIVER -> "receivers" to ((declaration as? FirCallableDeclaration)?.receiverParameter?.typeRef?.coneType ?: return)
            FILE, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, CONSTRUCTOR_PARAMETER, SETTER_PARAMETER, null -> when {
                declaration is FirProperty && !declaration.isLocal -> {
                    val allowedAnnotationTargets = annotation.getAllowedAnnotationTargets(context.session)
                    when {
                        declaration.fromPrimaryConstructor == true && allowedAnnotationTargets.contains(KotlinTarget.VALUE_PARAMETER) -> return // handled in FirValueParameter case
                        allowedAnnotationTargets.contains(KotlinTarget.PROPERTY) -> return
                        allowedAnnotationTargets.contains(KotlinTarget.FIELD) -> "fields" to declaration.returnTypeRef.coneType
                        else -> return
                    }
                }
                declaration is FirField -> "fields" to declaration.returnTypeRef.coneType
                declaration is FirValueParameter -> "parameters" to declaration.returnTypeRef.coneType
                declaration is FirVariable -> "variables" to declaration.returnTypeRef.coneType
                declaration is FirPropertyAccessor && declaration.isGetter && declaration.hasNoReceivers() ->
                    "getters" to declaration.returnTypeRef.coneType

                else -> return
            }
            ALL -> TODO() // How @all: interoperates with ValueClasses feature?
        }
        reportIfMfvc(context, reporter, annotation, hint, type)
    }

    private fun checkAnnotationTarget(
        declaration: FirAnnotationContainer,
        annotation: FirAnnotation,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
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
            checkAnnotationUseSiteTarget(declaration, annotation, useSiteTarget, applicableTargets, context, reporter)
        }

        if (check(actualTargets.defaultTargets) || check(actualTargets.canBeSubstituted) || checkWithUseSiteTargets()) {
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.ValueClasses)) {
                checkMultiFieldValueClassAnnotationRestrictions(declaration, annotation, context, reporter)
            }
            return
        }

        val targetDescription = actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"
        if (declaration is FirBackingField && actualTargets === AnnotationTargetLists.T_MEMBER_PROPERTY_IN_ANNOTATION &&
            !context.languageVersionSettings.supportsFeature(LanguageFeature.ForbidFieldAnnotationsOnAnnotationParameters)
        ) {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET_WARNING,
                targetDescription,
                applicableTargets,
                context
            )
        } else if (useSiteTarget != null) {
            if (useSiteTarget != ALL) {
                // We report specific diagnostics for ALL use-site target
                reporter.reportOn(
                    annotation.source,
                    FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                    targetDescription,
                    useSiteTarget.renderName,
                    applicableTargets,
                    context
                )
            }
        } else {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET,
                targetDescription,
                applicableTargets,
                context
            )
        }
    }

    private fun checkAnnotationUseSiteTarget(
        annotated: FirAnnotationContainer,
        annotation: FirAnnotation,
        target: AnnotationUseSiteTarget,
        applicableTargets: Set<KotlinTarget>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (annotation.source?.kind == KtFakeSourceElementKind.FromUseSiteTarget) return
        when (target) {
            PROPERTY,
            PROPERTY_GETTER -> {
                checkPropertyGetter(
                    annotated,
                    annotation,
                    target,
                    context,
                    reporter,
                    when (context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitUseSiteGetTargetAnnotations)) {
                        true -> FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY
                        false -> FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY_WARNING
                    }
                )
            }
            FIELD -> {
                if (annotated is FirBackingField) {
                    val propertySymbol = annotated.propertySymbol
                    if (propertySymbol.delegateFieldSymbol != null && !propertySymbol.hasBackingField) {
                        reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD, context)
                    }
                }
            }
            PROPERTY_DELEGATE_FIELD -> {
                if (annotated is FirBackingField && annotated.propertySymbol.delegateFieldSymbol == null) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE, context)
                }
            }
            PROPERTY_SETTER,
            SETTER_PARAMETER -> {
                if (!checkPropertyGetter(annotated, annotation, target, context, reporter, FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY) &&
                    !annotated.isVar
                ) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE, target.renderName, context)
                }
            }
            CONSTRUCTOR_PARAMETER -> when {
                annotated is FirValueParameter -> {
                    val container = context.containingDeclarations.lastOrNull()
                    if (container is FirConstructor && container.isPrimary) {
                        if (annotated.source?.hasValOrVar() != true) {
                            reporter.reportOn(annotation.source, FirErrors.REDUNDANT_ANNOTATION_TARGET, target.renderName, context)
                        }
                    } else {
                        reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET, context)
                    }
                }
                else -> reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET, context)
            }
            FILE -> {
                // NB: report once?
                if (annotated !is FirFile) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_FILE_TARGET, context)
                }
            }
            RECEIVER -> {
                // NB: report once?
                // annotation with use-site target `receiver` can be only on type reference, but not on declaration
                reporter.reportOn(
                    annotation.source,
                    FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                    "declaration",
                    target.renderName,
                    applicableTargets,
                    context
                )
            }
            ALL -> {
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.AnnotationAllUseSiteTarget)) {
                    when (annotated) {
                        is FirValueParameter -> {
                            if (annotated.correspondingProperty == null) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                    context
                                )
                            }
                        }
                        is FirProperty -> {
                            if (annotated.isLocal) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                    context
                                )
                            } else if (KotlinTarget.PROPERTY !in applicableTargets) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                                    "property",
                                    target.renderName,
                                    applicableTargets,
                                    context
                                )
                            }
                        }
                        else -> {
                            reporter.reportOn(
                                annotation.source,
                                FirErrors.INAPPLICABLE_ALL_TARGET,
                                context
                            )
                        }
                    }
                } else if (annotated !is FirValueParameter || annotated.correspondingProperty == null) {
                    // Condition is needed to avoid error duplication
                    reporter.reportOn(
                        annotation.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.AnnotationAllUseSiteTarget to context.languageVersionSettings,
                        context
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun checkPropertyGetter(
        annotated: FirAnnotationContainer,
        annotation: FirAnnotation,
        target: AnnotationUseSiteTarget,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        diagnostic: KtDiagnosticFactory1<String>
    ): Boolean {
        contract {
            returns(false) implies (annotated is FirProperty)
        }
        val isReport = annotated !is FirProperty || annotated.isLocal
        if (isReport) reporter.reportOn(annotation.source, diagnostic, target.renderName, context)
        return isReport
    }

    private fun checkDeprecatedCalls(
        deprecatedSinceKotlin: FirAnnotation,
        deprecated: FirAnnotation?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val closestFirFile = context.containingFile
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

    private fun checkDeclaredRepeatedAnnotations(
        annotationContainer: FirAnnotationContainer,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val annotationSources = annotationContainer.annotations.keysToMap { it.source }
        checkRepeatedAnnotation(
            annotationContainer, annotationContainer.annotations, context, reporter,
            annotationSources, defaultSource = null,
        )
    }

    private fun checkAllRepeatedAnnotations(
        typeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val annotationSources = typeRef.annotations.keysToMap { it.source }
        val useSiteSource = typeRef.source

        typeRef.coneType.forEachExpandedType(context.session) { type ->
            checkRepeatedAnnotation(null, type.typeAnnotations, context, reporter, annotationSources, useSiteSource)
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
            PROPERTY_GETTER to property.getter?.getAnnotationTypes(),
            PROPERTY_SETTER to property.setter?.getAnnotationTypes(),
            SETTER_PARAMETER to property.setter?.valueParameters?.single().getAnnotationTypes()
        )

        val isError = context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitRepeatedUseSiteTargetAnnotations)

        for (annotation in property.annotations) {
            val useSiteTarget = annotation.useSiteTarget ?: property.getDefaultUseSiteTarget(annotation, context)
            val existingAnnotations = propertyAnnotations[useSiteTarget] ?: continue

            if (annotation.annotationTypeRef.coneType in existingAnnotations && !annotation.isRepeatable(context.session)) {
                val factory = if (isError) FirErrors.REPEATED_ANNOTATION else FirErrors.REPEATED_ANNOTATION_WARNING
                if (annotation.source?.kind !is KtFakeSourceElementKind) {
                    reporter.reportOn(annotation.source, factory, context)
                }
            }
        }
    }

    private fun checkPossibleMigrationToPropertyOrField(
        parameter: FirValueParameter,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val session = context.session
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.AnnotationDefaultTargetMigrationWarning) ||
            // With this feature ON, the migration warning isn't needed
            session.languageVersionSettings.supportsFeature(LanguageFeature.PropertyParamAnnotationDefaultTargetMode)
        ) return
        val correspondingProperty = parameter.correspondingProperty ?: return

        for (annotation in parameter.annotations) {
            if (annotation.useSiteTarget != null) continue
            if (!annotation.requiresMigrationToPropertyOrFieldWarning(session)) continue
            val allowedTargets = annotation.useSiteTargetsFromMetaAnnotation(session)
            val propertyAllowed = PROPERTY in allowedTargets
            val fieldAllowed = FIELD in allowedTargets
            if (propertyAllowed || fieldAllowed) {
                if (propertyAllowed) {
                    reporter.reportOn(
                        annotation.source, FirErrors.ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD, PROPERTY.renderName, context
                    )
                } else if (correspondingProperty.backingField != null) {
                    val containingClass = context.containingDeclarations.getOrNull(context.containingDeclarations.size - 2) as? FirClass
                    if (containingClass?.classKind != ClassKind.ANNOTATION_CLASS) {
                        reporter.reportOn(
                            annotation.source, FirErrors.ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD, FIELD.renderName, context
                        )
                    }
                }
            }
        }
    }

    private fun FirAnnotation.requiresMigrationToPropertyOrFieldWarning(session: FirSession): Boolean {
        val symbol = toAnnotationClassLikeSymbol(session)
        val classId = symbol?.classId
        if (classId in STANDARD_ANNOTATION_IDS_WITHOUT_NECESSARY_MIGRATION) return false
        with(FirOptInUsageBaseChecker) {
            // To avoid additional warning together with existing OPT_IN_MARKER_ON_WRONG_TARGET
            if (symbol?.isExperimentalMarker(session) == true) return false
        }
        return true
    }

    private val JAVA_LANG_PACKAGE = FqName("java.lang")

    private val STANDARD_ANNOTATION_IDS_WITHOUT_NECESSARY_MIGRATION: Set<ClassId> = hashSetOf(
        OPT_IN_CLASS_ID,
        StandardClassIds.Annotations.Deprecated,
        StandardClassIds.Annotations.DeprecatedSinceKotlin,
        StandardClassIds.Annotations.Suppress,
        // Java stuff
        ClassId(JAVA_LANG_PACKAGE, Name.identifier("Deprecated")),
        ClassId(JAVA_LANG_PACKAGE, Name.identifier("SuppressWarnings")),
        // Below are the annotations we in principle want to ignore,
        // but looks like they can never arise here so they are commented
        // Allowed on ANNOTATION_CLASS only
        // REQUIRED_OPT_IN_CLASS_ID,
        // Allowed on CLASS only
        // SUBCLASS_OPT_IN_REQUIRED_CLASS_ID,
    )
}
