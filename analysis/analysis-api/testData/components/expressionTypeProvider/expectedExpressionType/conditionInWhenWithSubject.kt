enum class E {
    A
    B
}

fun foo(e: E) {
    when(e) {
        a<caret>v -> null
    }
}