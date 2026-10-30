// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47590
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -POTENTIALLY_NON_REPORTED_ANNOTATION
// LANGUAGE_FEATURE_TOGGLED: ForbidValueParameterTargetForOptInMarkers

import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@RequiresOptIn
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(VALUE_PARAMETER)<!>
annotation class Ann

@RequiresOptIn
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(CLASS, FUNCTION, VALUE_PARAMETER)<!>
annotation class AnnWithAllowedTargets

@RequiresOptIn
annotation class AnnWithoutTarget

@RequiresOptIn
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(TYPE)<!>
annotation class AnnWithType

@RequiresOptIn
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(TYPE, VALUE_PARAMETER)<!>
annotation class AnnWithTypeAndValueParameter

@Target(VALUE_PARAMETER)
annotation class RegularAnnotation

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference */
