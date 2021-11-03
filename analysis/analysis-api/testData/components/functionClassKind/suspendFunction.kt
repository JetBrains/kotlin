fun foo(pause : suspend () -> Unit) {
    pause()
}

fun bar() {
    foo(x<caret>y)
}
