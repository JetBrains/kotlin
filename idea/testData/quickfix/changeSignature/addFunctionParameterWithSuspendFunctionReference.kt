// "Add parameter to function 'baz'" "true"
suspend fun bar(): Int = 42

suspend fun baz() {}

suspend fun foo() {
    baz(::bar<caret>)
}