fun foo(p: Int) {
    "abc".fold(1) { <caret> }
}

// EXIST: p
// EXIST: "acc: Int, c: Char ->"
// EXIST: "acc, c ->"
