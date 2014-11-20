enum class E {
  A
  B
  C
}

fun foo(e: E) {
    when(e) {
        E.A -> x()
        <caret>
    }
}

// ABSENT: A
// EXIST: { lookupString:"B", itemText:"E.B" }
// EXIST: { lookupString:"C", itemText:"E.C" }
// EXIST: {"lookupString":"else","tailText":" ->","itemText":"else"}
