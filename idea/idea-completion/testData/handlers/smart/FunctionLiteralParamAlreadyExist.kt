fun foo(p: Int, handler: () -> Unit){}

fun bar(p: Int) {
    foo(<caret>) { doIt() }
}

// ELEMENT: p
