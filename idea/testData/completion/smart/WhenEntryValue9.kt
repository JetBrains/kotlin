fun foo(p1: String, p2: String) {
    when (p1) {
        <caret>
    }
}

// ABSENT: p1
// EXIST: p2
