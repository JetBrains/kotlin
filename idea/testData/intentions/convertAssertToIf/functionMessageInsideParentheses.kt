// WITH_RUNTIME
fun foo() {
    <caret>assert(true, ::message)
}

fun message(): String = "text"