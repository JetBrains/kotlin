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
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.forEachExpandedType
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
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

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration is FirDanglingModifierList) {
            return
        }

        checkAnnotationContainer(declaration)

        if (declaration is FirCallableDeclaration) {
            declaration.receiverParameter?.let {
                checkAnnotationContainer(it)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationContainer(declaration: FirAnnotationContainer) {
        var deprecated: FirAnnotation? = null
        var deprecatedSinceKotlin: FirAnnotation? = null

        for (annotation in declaration.annotations) {
            val fqName = annotation.fqName(context.session) ?: continue
            if (fqName == deprecatedClassId) {
                deprecated = annotation
            } else if (fqName == deprecatedSinceKotlinClassId) {
                deprecatedSinceKotlin = annotation
            }

            checkAnnotationTarget(declaration, annotation)
        }

        if (declaration is FirCallableDeclaration) {
            val receiverParameter = declaration.receiverParameter
            if (receiverParameter != null) {
                for (receiverAnnotation in receiverParameter.annotations) {
                    reportIfMfvc(receiverAnnotation, "receivers", receiverParameter.typeRef.coneType)
                }
            }
        }

        if (deprecatedSinceKotlin != null) {
            checkDeprecatedCalls(deprecatedSinceKotlin, deprecated)
        }

        checkDeclaredRepeatedAnnotations(declaration)

        if (declaration is FirCallableDeclaration) {
            if (declaration is FirProperty) {
                checkRepeatedAnnotationsInProperty(declaration)
            }
            if (declaration is FirValueParameter) {
                checkPossibleMigrationToPropertyOrField(declaration)
            }

            if (declaration.source?.kind is KtRealSourceElementKind && declaration.returnTypeRef.source?.kind is KtRealSourceElementKind) {
                checkAllRepeatedAnnotations(declaration.returnTypeRef)
            }
        } else if (declaration is FirTypeAlias) {
            checkAllRepeatedAnnotations(declaration.expandedTypeRef)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIfMfvc(
        annotation: FirAnnotation,
        hint: String,
        type: ConeKotlinType,
    ) {
        if (type.needsMultiFieldValueClassFlattening(context.session)) {
            reporter.reportOn(annotation.source, FirErrors.ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET, hint)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMultiFieldValueClassAnnotationRestrictions(declaration: FirAnnotationContainer, annotation: FirAnnotation) {
        fun FirPropertyAccessor.hasNoReceivers() = contextParameters.isEmpty() && receiverParameter?.typeRef == null &&
                !propertySymbol.isExtension && !propertySymbol.hasContextParameters

        val (hint, type) = when (annotation.useSiteTarget) {
            FIELD -> "fields" to ((declaration as? FirBackingField)?.returnTypeRef?.coneType ?: return)
            PROPERTY_DELEGATE_FIELD -> "delegate fields" to ((declaration as? FirBackingField)?.propertySymbol?.delegate?.resolvedType
                ?: return)
            RECEIVER -> "receivers" to ((declaration as? FirCallableDeclaration)?.receiverParameter?.typeRef?.coneType ?: return)
            FILE, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, CONSTRUCTOR_PARAMETER, SETTER_PARAMETER, null -> when (declaration) {
                is FirProperty if declaration.symbol is FirRegularPropertySymbol -> {
                    val allowedAnnotationTargets = annotation.getAllowedAnnotationTargets(context.session)
                    when {
                        declaration.fromPrimaryConstructor == true && allowedAnnotationTargets.contains(KotlinTarget.VALUE_PARAMETER) -> return // handled in FirValueParameter case
                        allowedAnnotationTargets.contains(KotlinTarget.PROPERTY) -> return
                        allowedAnnotationTargets.contains(KotlinTarget.FIELD) -> "fields" to declaration.returnTypeRef.coneType
                        else -> return
                    }
                }
                is FirField -> "fields" to declaration.returnTypeRef.coneType
                is FirValueParameter -> "parameters" to declaration.returnTypeRef.coneType
                is FirVariable -> "variables" to declaration.returnTypeRef.coneType
                is FirPropertyAccessor if declaration.isGetter && declaration.hasNoReceivers() -> "getters" to declaration.returnTypeRef.coneType
                else -> return
            }
            ALL -> TODO() // How @all: interoperates with ValueClasses feature?
        }
        reportIfMfvc(annotation, hint, type)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationTarget(declaration: FirAnnotationContainer, annotation: FirAnnotation) {
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
            checkAnnotationUseSiteTarget(declaration, annotation, useSiteTarget, applicableTargets)
        }

        if (check(actualTargets.defaultTargets) || check(actualTargets.canBeSubstituted) || checkWithUseSiteTargets()) {
            if (LanguageFeature.ValueClasses.isEnabled()) {
                checkMultiFieldValueClassAnnotationRestrictions(declaration, annotation)
            }
            return
        }

        val targetDescription = actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"
        if (declaration is FirBackingField && actualTargets === AnnotationTargetLists.T_MEMBER_PROPERTY_IN_ANNOTATION &&
            !LanguageFeature.ForbidFieldAnnotationsOnAnnotationParameters.isEnabled()
        ) {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET_WARNING,
                targetDescription,
                applicableTargets,
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
                )
            }
        } else {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET,
                targetDescription,
                applicableTargets,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationUseSiteTarget(
        annotated: FirAnnotationContainer,
        annotation: FirAnnotation,
        target: AnnotationUseSiteTarget,
        applicableTargets: Set<KotlinTarget>,
    ) {
        if (annotation.source?.kind == KtFakeSourceElementKind.FromUseSiteTarget) return
        when (target) {
            PROPERTY,
            PROPERTY_GETTER -> {
                checkPropertyGetter(
                    annotated,
                    annotation,
                    target,
                    when (LanguageFeature.ProhibitUseSiteGetTargetAnnotations.isEnabled()) {
                        true -> FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY
                        false -> FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY_WARNING
                    }
                )
            }
            FIELD -> {
                if (annotated is FirBackingField) {
                    val propertySymbol = annotated.propertySymbol
                    if (propertySymbol.delegateFieldSymbol != null && !propertySymbol.hasBackingField) {
                        reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD)
                    }
                }
            }
            PROPERTY_DELEGATE_FIELD -> {
                if (annotated is FirBackingField && annotated.propertySymbol.delegateFieldSymbol == null) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE)
                }
            }
            PROPERTY_SETTER,
            SETTER_PARAMETER -> {
                if (!checkPropertyGetter(annotated, annotation, target, FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY) &&
                    !annotated.isVar
                ) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE, target.renderName)
                }
            }
            CONSTRUCTOR_PARAMETER -> when {
                annotated is FirValueParameter -> {
                    val container = context.containingDeclarations.lastOrNull()
                    if (container.isPrimaryConstructor()) {
                        if (annotated.source?.hasValOrVar() != true) {
                            reporter.reportOn(annotation.source, FirErrors.REDUNDANT_ANNOTATION_TARGET, target.renderName)
                        }
                    } else {
                        reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET)
                    }
                }
                else -> reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET)
            }
            FILE -> {
                // NB: report once?
                if (annotated !is FirFile) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_FILE_TARGET)
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
                )
            }
            ALL -> {
                if (LanguageFeature.AnnotationAllUseSiteTarget.isEnabled()) {
                    when (annotated) {
                        is FirValueParameter -> {
                            if (annotated.correspondingProperty == null) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                )
                            }
                        }
                        is FirProperty -> {
                            if (annotated.symbol is FirLocalPropertySymbol) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                )
                            } else if (annotated.delegate != null) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                )
                            } else if (KotlinTarget.PROPERTY !in applicableTargets) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                                    "property",
                                    target.renderName,
                                    applicableTargets,
                                )
                            }
                        }
                        else -> {
                            reporter.reportOn(
                                annotation.source,
                                FirErrors.INAPPLICABLE_ALL_TARGET,
                            )
                        }
                    }
                } else if (annotated !is FirValueParameter || annotated.correspondingProperty == null) {
                    // Condition is needed to avoid error duplication
                    reporter.reportOn(
                        annotation.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.AnnotationAllUseSiteTarget to context.languageVersionSettings,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyGetter(
        annotated: FirAnnotationContainer,
        annotation: FirAnnotation,
        target: AnnotationUseSiteTarget,
        diagnostic: KtDiagnosticFactory1<String>
    ): Boolean {
        contract {
            returns(false) implies (annotated is FirProperty)
        }
        val isReport = annotated !is FirProperty || annotated.symbol is FirLocalPropertySymbol
        if (isReport) reporter.reportOn(annotation.source, diagnostic, target.renderName)
        return isReport
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDeprecatedCalls(deprecatedSinceKotlin: FirAnnotation, deprecated: FirAnnotation?) {
        val closestFirFile = context.containingFileSymbol
        if (closestFirFile != null && !closestFirFile.packageFqName.startsWith(StandardClassIds.BASE_KOTLIN_PACKAGE.shortName())) {
            reporter.reportOn(
                deprecatedSinceKotlin.source,
                FirErrors.DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE,
            )
        }

        if (deprecated == null) {
            reporter.reportOn(deprecatedSinceKotlin.source, FirErrors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED)
        } else {
            val argumentMapping = deprecated.argumentMapping.mapping
            for (name in argumentMapping.keys) {
                if (name.identifier == "level") {
                    reporter.reportOn(
                        deprecatedSinceKotlin.source,
                        FirErrors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL,
                    )
                    break
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDeclaredRepeatedAnnotations(annotationContainer: FirAnnotationContainer) {
        val annotationSources = annotationContainer.annotations.keysToMap { it.source }
        checkRepeatedAnnotation(
            annotationContainer, annotationContainer.annotations,
            annotationSources, defaultSource = null,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAllRepeatedAnnotations(typeRef: FirTypeRef) {
        val annotationSources = typeRef.annotations.keysToMap { it.source }
        val useSiteSource = typeRef.source

        typeRef.coneType.forEachExpandedType(context.session) { type ->
            checkRepeatedAnnotation(null, type.typeAnnotations, annotationSources, useSiteSource)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkRepeatedAnnotationsInProperty(property: FirProperty) {
        fun FirAnnotationContainer?.getAnnotationTypes(): List<ConeKotlinType> {
            return this?.annotations?.map { it.annotationTypeRef.coneType } ?: listOf()
        }

        val propertyAnnotations = mapOf(
            PROPERTY_GETTER to property.getter?.getAnnotationTypes(),
            PROPERTY_SETTER to property.setter?.getAnnotationTypes(),
            SETTER_PARAMETER to property.setter?.valueParameters?.single().getAnnotationTypes()
        )

        for (annotation in property.annotations) {
            val useSiteTarget = annotation.useSiteTarget ?: property.getDefaultUseSiteTarget(annotation)
            val existingAnnotations = propertyAnnotations[useSiteTarget] ?: continue

            if (annotation.annotationTypeRef.coneType in existingAnnotations && !annotation.isRepeatable(context.session)) {
                if (annotation.source?.kind !is KtFakeSourceElementKind) {
                    reporter.reportOn(annotation.source, FirErrors.REPEATED_ANNOTATION)
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPossibleMigrationToPropertyOrField(parameter: FirValueParameter) {
        val session = context.session
        if (!LanguageFeature.AnnotationDefaultTargetMigrationWarning.isEnabled() ||
            // With this feature ON, the migration warning isn't needed
            LanguageFeature.PropertyParamAnnotationDefaultTargetMode.isEnabled()
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
                        annotation.source, FirErrors.ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD, PROPERTY.renderName
                    )
                } else if (correspondingProperty.backingField != null) {
                    val containingClass = context.containingDeclarations.getOrNull(context.containingDeclarations.size - 2) as? FirClassSymbol<*>
                    if (containingClass?.classKind != ClassKind.ANNOTATION_CLASS) {
                        reporter.reportOn(
                            annotation.source, FirErrors.ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD, FIELD.renderName
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
