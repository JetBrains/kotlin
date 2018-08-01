// "Change existent retention to SOURCE" "false"
// DISABLE-ERRORS
// ACTION: Add SOURCE retention
// ACTION: Make internal
// ACTION: Make private
// ACTION: Remove EXPRESSION target
<caret>@Target(AnnotationTarget.EXPRESSION)
annotation class Ann