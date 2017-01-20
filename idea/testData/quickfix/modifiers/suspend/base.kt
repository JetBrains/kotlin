// "Make bar suspend" "true"

suspend fun foo() {}
fun bar() {
    <caret>foo()
}