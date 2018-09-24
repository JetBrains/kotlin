// "Remove @ from annotation argument" "true"

annotation class Y()
annotation class X(val value: Y, val y: Y)

@X(Y(), y = @Y()<caret>)
fun foo() {

}