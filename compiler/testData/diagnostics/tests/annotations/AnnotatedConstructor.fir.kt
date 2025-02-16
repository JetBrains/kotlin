// RUN_PIPELINE_TILL: BACKEND
annotation class ann
class Annotated(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@ann<!> val x: Int)
