// WITH_RUNTIME
// INTENTION_TEXT: Remove @ used as annotation argument

annotation class X(val value: Array<Y>)
annotation class Y()

@X(arrayOf(@Y(), @Y(), @Y())<caret>)
fun foo() {
}