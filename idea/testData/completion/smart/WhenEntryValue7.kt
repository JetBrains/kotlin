enum class E {
  A
  B
  C
}

fun foo(e: E) {
    when(e) {
        <caret>
        E.B -> x()
    }
}

// EXIST: E.A
// ABSENT: else
