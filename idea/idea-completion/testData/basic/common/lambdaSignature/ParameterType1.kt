fun bar() {
    val handler = { (i: <caret>) }
}

// EXIST: Int
// EXIST: String
// ABSENT: bar
// ABSENT: handler