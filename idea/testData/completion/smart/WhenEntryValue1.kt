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

// EXIST: { lookupString:"A", itemText:"E.A" }
// EXIST: { lookupString:"B", itemText:"E.B" }
// EXIST: { lookupString:"C", itemText:"E.C" }
// EXIST: {"lookupString":"else","tailText":" ->","itemText":"else"}
