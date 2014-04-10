fun foo(s: String, i: Int){ }

fun bar(sss: String) {
    foo(<caret>a, b)
}

//ELEMENT: sss
//CHAR: \t
