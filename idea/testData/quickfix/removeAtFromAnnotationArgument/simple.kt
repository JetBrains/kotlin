// "Remove @ from annotation argument" "true"

annotation class Y()
annotation class X(val value: Y)

@X(@Y()<caret>)
fun foo() {
}