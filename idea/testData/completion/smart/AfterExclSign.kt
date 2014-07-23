fun foo(p1: Boolean, p2: String) {
    if (!<caret>)
}

// EXIST: p1
// ABSENT: p2
