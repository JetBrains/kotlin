fun foo() {
    val a: (Int) -> Unit = { <caret>a -> bar(a) }
}

fun bar(i: Int) {}