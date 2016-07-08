// WITH_RUNTIME
// INTENTION_TEXT: Remove @ used as annotation argument

annotation class X(val value: Y)
annotation class Y()

@X(@Y()<caret>)
fun foo() {
}