fun foo(s: String, list: List<String>, o: Any) {
    if (s in <caret>)
}

// EXIST: list
// ABSENT: o
