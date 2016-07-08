// IS_APPLICABLE: false

annotation class X(val s: String)

@X("@@@"<caret>)
fun foo() {
}