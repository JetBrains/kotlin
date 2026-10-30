// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47590
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -POTENTIALLY_NON_REPORTED_ANNOTATION

import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@RequiresOptIn
<!OPT_IN_MARKER_WITH_WRONG_TARGET!>@Target(VALUE_PARAMETER)<!>
annotation class Ann

/* GENERATED_FIR_TAGS: annotationDeclaration */
