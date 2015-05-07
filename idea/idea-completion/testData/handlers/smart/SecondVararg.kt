fun foo(vararg v1: Int, vararg v2: Char, s: String) { }

fun bar(c: Char) {
    foo(*intArrayOf(), <caret>)
}

// ELEMENT: c
