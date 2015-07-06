fun bar() {
    val handler = { i: Int, s: <caret> }
}

// EXIST: Int
// EXIST: String
// ABSENT: bar
// ABSENT: handler