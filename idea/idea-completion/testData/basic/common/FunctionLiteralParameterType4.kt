fun bar() {
    val handler = { (i: List<<caret>>) }
}

// EXIST: Int
// EXIST: String
// ABSENT: bar
// ABSENT: handler