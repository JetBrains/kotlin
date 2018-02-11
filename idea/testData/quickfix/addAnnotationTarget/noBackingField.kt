// "Add annotation target" "true"

@Target(AnnotationTarget.FIELD)
annotation class FieldAnn

<caret>@FieldAnn
val x get() = 42