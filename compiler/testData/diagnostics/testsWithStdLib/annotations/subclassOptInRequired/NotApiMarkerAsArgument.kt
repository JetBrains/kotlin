// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

annotation class NotOptInAnnotation

@RequiresOptIn
annotation class OptInAnnotation

@SubclassOptInRequired(<!SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER!>NotOptInAnnotation::class<!>)
open class IncorrectSubclassOptInArgumentMarkerA

@SubclassOptInRequired(OptInAnnotation::class, <!SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER!>NotOptInAnnotation::class<!>)
open class IncorrectSubclassOptInArgumentMarkerB
