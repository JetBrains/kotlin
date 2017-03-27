// WITH_RUNTIME
// INTENTION_TEXT: Remove @ from annotation argument
// SKIP_ERRORS_BEFORE

annotation class X(val value: Array<Y>)
annotation class Y()

@X(arrayOf(Y(), @Y()<caret>))
fun foo() {
}