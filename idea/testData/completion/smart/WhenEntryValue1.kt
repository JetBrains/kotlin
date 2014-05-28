enum class E {
    A
    B
    C
}

fun foo(e: E) {
    when(e) {
        <caret>
    }
}

// EXIST: E.A
// EXIST: E.B
// EXIST: E.C
// EXIST: {"lookupString":"else","tailText":" ->","itemText":"else"}
