fun foo(p: () -> Unit)){}
fun foo(p: () -> (() -> Unit)){}

fun bar() {
    foo(<caret>)
}

fun f(): () -> Unit {}

// EXIST: ::f
// EXIST: f
