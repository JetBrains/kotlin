enum class E {
    A
    B
}

fun foo(e: E) {
    val result = when(e) {
        E.A -> 1
        E.B -> a<caret>v
    }
}