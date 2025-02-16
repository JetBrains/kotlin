// RUN_PIPELINE_TILL: BACKEND
// Class constructor parameter CAN be recursively annotated
annotation class RecursivelyAnnotated(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD!>@RecursivelyAnnotated(1)<!> val x: Int)
