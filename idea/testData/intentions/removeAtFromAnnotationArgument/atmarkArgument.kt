// INTENTION_TEXT: Remove @ from annotation argument

annotation class X(val value: Y)
annotation class Y()

@X(@Y()<caret>)
fun foo() {
}