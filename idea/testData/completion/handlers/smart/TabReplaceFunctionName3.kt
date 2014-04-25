fun foo(s: String, i: Int){ }

fun takeString(p: Int): String = ""

fun bar() {
    foo(<caret>x(1))
}

//ELEMENT: takeString
//CHAR: \t
