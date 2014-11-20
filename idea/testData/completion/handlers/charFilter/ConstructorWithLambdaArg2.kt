class C(filter: (String) -> Boolean)

fun foo(p: C) {
    val c: C = <caret>
}

// COMPLETION_TYPE: SMART
// ELEMENT: C
// CHAR: (
