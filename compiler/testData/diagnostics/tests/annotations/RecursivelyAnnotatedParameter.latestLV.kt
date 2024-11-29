// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE
// Class constructor parameter CAN be recursively annotated
annotation class RecursivelyAnnotated(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD!>@RecursivelyAnnotated(1)<!> val x: Int)
