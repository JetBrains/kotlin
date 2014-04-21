fun foo(s: String, c: Char){ }

fun bar(sss: String?) {
    foo(<caret>)
}

// ELEMENT_TEXT: "!! sss"
