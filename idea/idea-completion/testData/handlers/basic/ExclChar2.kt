fun String.checkIt(s: String): Boolean = true

fun foo(s: String) {
    if (s.<caret>)
}

// ELEMENT: checkIt
// CHAR: '!'
