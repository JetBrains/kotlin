// !DIAGNOSTICS_NUMBER: 2
// !DIAGNOSTICS: INCOMPATIBLE_ENUM_COMPARISON_ERROR
// !MESSAGE_TYPE: TEXT

enum class E1 {
    A, B
}

enum class E2 {
    A, B
}

fun foo1(e1: E1, e2: E2) {
    e1 == e2
}

fun foo2(e1: E1, e2: E2) {
    when (e1) {
        E2.A -> {}
        else -> {}
    }
}
