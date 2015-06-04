fun foo(vararg v: Int, s: String) { }

fun bar(i: Int) {
    foo(*intArrayOf(), <caret>)
}

// ELEMENT: i
