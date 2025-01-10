// RUN_PIPELINE_TILL: FRONTEND
// Class constructor parameter CAN be recursively annotated
class RecursivelyAnnotated(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@<!NOT_AN_ANNOTATION_CLASS!>RecursivelyAnnotated<!>(1)<!> val x: Int)
