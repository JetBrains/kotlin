fun foo(p: Int): Int = p

fun f(p: Int): Int {
    return <caret>foo(1)
}

//ELEMENT: p
