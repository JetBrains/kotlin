fun foo(vararg v: Int, s: String) { }

fun bar(s: String) {
    foo(*intArrayOf(), <caret>)
}

// ELEMENT: s
