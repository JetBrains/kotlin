fun foo(p: (Int) -> Unit) { }

fun fff1(p: Int): Nothing{}
fun fff2(p: Int): Unit{}

fun f() {
    foo(::fff<caret>)
}

// ORDER: fff2
// ORDER: fff1
