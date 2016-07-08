// WITH_RUNTIME
// INTENTION_TEXT: Remove @ used as annotation argument

annotation class X(val value: Y)
annotation class Y()
annotation class Z(val value: Y)

@Y
@X(@Y()<caret>)
@Z(@Y())
fun foo() {
}