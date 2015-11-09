// WITH_RUNTIME
fun foo() {
    <caret>assert(bar()) { "text" }
}

fun bar(): Boolean = true