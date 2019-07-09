// "Add SOURCE retention" "false"
// DISABLE-ERRORS
// ACTION: Change existent retention to SOURCE
// ACTION: Make internal
// ACTION: Make private
// ACTION: Remove EXPRESSION target
<caret>@Retention
@Target(AnnotationTarget.EXPRESSION)
annotation class Ann