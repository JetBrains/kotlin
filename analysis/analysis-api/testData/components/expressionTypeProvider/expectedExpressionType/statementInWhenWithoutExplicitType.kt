enum class E {
    A
    B
    C
    D
}

fun foo(e: E) = when (e) {
    E.A -> 1
    E.B -> ""
    E.C -> a<caret>v
    E.D -> unresolved
}
