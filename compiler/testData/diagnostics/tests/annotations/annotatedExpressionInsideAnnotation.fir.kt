// SKIP_ERRORS_BEFORE

annotation class X(val value: Y, val y: Y)
annotation class Y()

@X(@Y()<!SYNTAX!><!>, y = Y())
fun foo1() {
}
@X(@Y()<!SYNTAX!><!>, y = @Y()<!SYNTAX!><!>)
fun foo2() {
}
