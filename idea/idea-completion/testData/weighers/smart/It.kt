fun foo(xx: String){}

fun bar(handler: (String) -> Unit){}

fun f() {
    val v = ""
    val xx = ""
    bar { foo(<caret>) }
}

// ORDER: xx
// ORDER: it
// ORDER: v
