// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

@RequiresOptIn
annotation class Marker

@Marker
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(Marker::class)
interface MessApi

open class MessImpl: <!OPT_IN_TO_INHERITANCE_ERROR, OPT_IN_USAGE_ERROR!>MessApi<!>
