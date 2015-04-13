fun foo(list: MutableMap<String, Int>, p1: Any, p2: String) {
    list.remove(<caret>)
}

// ABSENT: p1
// EXIST: p2
