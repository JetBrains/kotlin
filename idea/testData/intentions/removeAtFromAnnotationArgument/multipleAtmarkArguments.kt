// INTENTION_TEXT: Remove @ from annotation argument

annotation class X(val value: Y, val y: Y)
annotation class Y()

@X(@Y(), y = @Y()<caret>)
fun foo() {
}