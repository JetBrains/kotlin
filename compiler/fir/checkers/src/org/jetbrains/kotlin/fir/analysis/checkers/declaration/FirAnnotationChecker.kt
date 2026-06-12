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
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.hasValOrVar
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.checkRepeatedAnnotation
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.forEachExpandedType
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
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

        val annotationsWithIncorrectTarget = mutableSetOf<FirAnnotation>()
        for (annotation in declaration.annotations) {
            val fqName = annotation.fqName(context.session) ?: continue
            if (fqName == deprecatedClassId) {
                deprecated = annotation
            } else if (fqName == deprecatedSinceKotlinClassId) {
                deprecatedSinceKotlin = annotation
            }

            if (checkAnnotationTarget(declaration, annotation)) {
                annotationsWithIncorrectTarget += annotation
            }
        }

        if (deprecatedSinceKotlin != null) {
            checkDeprecatedCalls(deprecatedSinceKotlin, deprecated)
        }

        val annotations = declaration.annotations - annotationsWithIncorrectTarget
        checkRepeatedAnnotation(annotations, annotations.keysToMap { it.source }, defaultSource = null)

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
    ): Boolean {
        if (type.needsJvmInlineMultiFieldValueClassFlattening(context.session)) {
            reporter.reportOn(annotation.source, FirErrors.ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET, hint)
            return true
        } else return false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMultiFieldValueClassAnnotationRestrictions(declaration: FirAnnotationContainer, annotation: FirAnnotation): Boolean {
        fun FirPropertyAccessor.hasNoReceivers() = contextParameters.isEmpty() && receiverParameter?.typeRef == null &&
                !propertySymbol.isExtension && !propertySymbol.hasContextParameters

        val [hint, type] = if (annotation.useSiteTarget == PROPERTY_DELEGATE_FIELD) {
            // The only target that requires additional handling because both FIELD and PROPERTY_DELEGATE_FIELD use FirBackingField
            "delegate fields" to ((declaration as? FirBackingField)?.propertySymbol?.delegate?.resolvedType
                ?: return false)
        } else when (declaration) {
            is FirReceiverParameter -> "receivers" to declaration.typeRef.coneType
            is FirProperty if declaration.symbol is FirRegularPropertySymbol -> return false
            is FirField -> "fields" to declaration.returnTypeRef.coneType // This includes also FirBackingField
            is FirValueParameter -> "parameters" to declaration.returnTypeRef.coneType
            is FirVariable -> "variables" to declaration.returnTypeRef.coneType
            is FirPropertyAccessor if declaration.isGetter && declaration.hasNoReceivers() -> "getters" to declaration.returnTypeRef.coneType
            else -> return false
        }
        return reportIfMfvc(annotation, hint, type)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationTarget(declaration: FirAnnotationContainer, annotation: FirAnnotation): Boolean {
        val actualTargets = getActualTargetList(declaration)
        val applicableTargets = annotation.getAllowedAnnotationTargets(context.session)
        val useSiteTarget = annotation.useSiteTarget
        var incorrectTarget = false

        fun check(targets: List<KotlinTarget>) = targets.any {
            it in applicableTargets && (useSiteTarget == null || KotlinTarget.USE_SITE_MAPPING[useSiteTarget] == it)
        }

        fun checkWithUseSiteTargets(): Boolean {
            if (useSiteTarget == null) return false

            val useSiteMapping = KotlinTarget.USE_SITE_MAPPING[useSiteTarget]
            return actualTargets.onlyWithUseSiteTarget.any { it in applicableTargets && it == useSiteMapping }
        }

        if (useSiteTarget != null) {
            incorrectTarget = checkAnnotationUseSiteTarget(declaration, annotation, useSiteTarget, applicableTargets)
        }

        if (check(actualTargets.defaultTargets) || check(actualTargets.canBeSubstituted) || checkWithUseSiteTargets()) {
            if (LanguageFeature.JvmInlineMultiFieldValueClasses.isEnabled()) {
                checkMultiFieldValueClassAnnotationRestrictions(declaration, annotation)
            }
            return incorrectTarget
        }

        val targetDescription = actualTargets.defaultTargets.firstOrNull()?.description ?: "unidentified target"
        if (declaration is FirBackingField && actualTargets === AnnotationTargetLists.T_MEMBER_PROPERTY_IN_ANNOTATION &&
            LanguageFeature.ForbidFieldAnnotationsOnAnnotationParameters.isDisabled()
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
                incorrectTarget = true
            }
        } else {
            reporter.reportOn(
                annotation.source,
                FirErrors.WRONG_ANNOTATION_TARGET,
                targetDescription,
                applicableTargets,
            )
            incorrectTarget = true
        }
        return incorrectTarget
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationUseSiteTarget(
        annotated: FirAnnotationContainer,
        annotation: FirAnnotation,
        target: AnnotationUseSiteTarget,
        applicableTargets: Set<KotlinTarget>,
    ): Boolean {
        if (annotation.source?.kind == KtFakeSourceElementKind.FromUseSiteTarget) return false
        return when (target) {
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
                        return true
                    }
                }
                false
            }
            PROPERTY_DELEGATE_FIELD -> {
                if (annotated is FirBackingField && annotated.propertySymbol.delegateFieldSymbol == null) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE)
                    true
                } else false
            }
            PROPERTY_SETTER,
            SETTER_PARAMETER -> when {
                checkPropertyGetter(annotated, annotation, target, FirErrors.INAPPLICABLE_TARGET_ON_PROPERTY) -> {
                    true
                }
                !annotated.isVar -> {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE, target.renderName)
                    true
                }
                else -> false
            }
            CONSTRUCTOR_PARAMETER -> when {
                annotated is FirValueParameter -> {
                    val container = context.containingDeclarations.lastOrNull()
                    if (container.isPrimaryConstructor()) {
                        if (annotated.source?.hasValOrVar() != true) {
                            reporter.reportOn(annotation.source, FirErrors.REDUNDANT_ANNOTATION_TARGET, target.renderName)
                        }
                        false
                    } else {
                        reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET)
                        true
                    }
                }
                else -> {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_PARAM_TARGET)
                    true
                }
            }
            FILE -> {
                // NB: report once?
                if (annotated !is FirFile) {
                    reporter.reportOn(annotation.source, FirErrors.INAPPLICABLE_FILE_TARGET)
                    true
                } else false
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
                true
            }
            ALL -> {
                if (LanguageFeature.AnnotationAllUseSiteTarget.isEnabled()) {
                    when (annotated) {
                        is FirValueParameter -> {
                            if (annotated.correspondingProperty == null) {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                    if (annotated.containingDeclarationSymbol is FirConstructorSymbol) {
                                        "constructor parameters without corresponding property (consider adding val/var)"
                                    } else {
                                        "value parameters, only properties are allowed"
                                    },
                                )
                                return true
                            }
                            false
                        }
                        is FirProperty -> when {
                            annotated.symbol is FirLocalPropertySymbol -> {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                    "local properties, only member or top-level properties are allowed",
                                )
                                true
                            }
                            annotated.delegate != null -> {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.INAPPLICABLE_ALL_TARGET,
                                    "delegated properties",
                                )
                                true
                            }
                            KotlinTarget.PROPERTY !in applicableTargets -> {
                                reporter.reportOn(
                                    annotation.source,
                                    FirErrors.WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET,
                                    "property",
                                    target.renderName,
                                    applicableTargets,
                                )
                                true
                            }
                            else -> false
                        }
                        else -> {
                            reporter.reportOn(
                                annotation.source,
                                FirErrors.INAPPLICABLE_ALL_TARGET,
                                "elements other than properties",
                            )
                            true
                        }
                    }
                } else if (annotated !is FirValueParameter || annotated.correspondingProperty == null) {
                    // Condition is needed to avoid error duplication
                    reporter.reportOn(
                        annotation.source,
                        FirErrors.UNSUPPORTED_FEATURE,
                        LanguageFeature.AnnotationAllUseSiteTarget to context.languageVersionSettings,
                    )
                    true
                } else false
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
        return isReport && diagnostic.severity == Severity.ERROR
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
    private fun checkAllRepeatedAnnotations(typeRef: FirTypeRef) {
        val annotationSources = typeRef.annotations.keysToMap { it.source }
        val useSiteSource = typeRef.source

        typeRef.coneType.forEachExpandedType(context.session) { type ->
            checkRepeatedAnnotation(type.typeAnnotations, annotationSources, useSiteSource)
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
        if (LanguageFeature.AnnotationDefaultTargetMigrationWarning.isDisabled() ||
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
