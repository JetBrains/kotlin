// "Make bar suspend" "true"

suspend fun foo() {}
public fun bar() {
    <caret>foo()
}