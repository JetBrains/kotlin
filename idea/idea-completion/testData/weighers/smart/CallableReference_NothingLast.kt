fun foo(p: (Int) -> Unit) { }

fun fff1(p: Int): Nothing{}
fun fff2(p: Int): Unit{}

fun f() {
    foo(<caret>)
}

// ORDER: {...}
// ORDER: { Int -> ... }
// ORDER: ::fff2
