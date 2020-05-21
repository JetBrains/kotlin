// "Make containing function suspend" "false"
// ACTION: Convert to run
// ACTION: Convert to with
// DISABLE-ERRORS
class A {
    suspend fun foo() {}
}
val p = fun(a: A) {
    a.<caret>foo()
}