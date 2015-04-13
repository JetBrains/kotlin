fun foo(p: () -> Unit, i: Int){}

fun bar() {
    foo(<caret>)
}

fun f(){}

// ELEMENT: ::f