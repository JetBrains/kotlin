fun foo() {
    listOf(1, 2).map { <caret> }
}

// EXIST: "i ->"
// EXIST: "i: Int ->"
