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

// ABSENT: A
// EXIST: B
// EXIST: C
// EXIST: null
// EXIST: {"lookupString":"else","tailText":" ->","itemText":"else"}
