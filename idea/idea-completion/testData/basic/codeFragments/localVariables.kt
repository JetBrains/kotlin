fun foo() {
    val aaa1 = 1
    <caret>val t = 2
    val aaa3 = 2
}

// INVOCATION_COUNT: 1
// EXIST: aaa1
// ABSENT: aaa3
