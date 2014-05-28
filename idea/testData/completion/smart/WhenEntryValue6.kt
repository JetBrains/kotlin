enum class E {
  A
  B
  C
}

fun foo(e: E?) {
    when(e) {
        E.A -> x()
        <caret>
    }
}

// ABSENT: E.A
// EXIST: E.B
// EXIST: E.C
// EXIST: null
// EXIST: {"lookupString":"else","tailText":" ->","itemText":"else"}
