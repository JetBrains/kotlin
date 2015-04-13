fun foo() {
    val aaabbbccc = 1
    aaa<caret>bbbccc
}

// INVOCATION_COUNT: 1
// EXIST: aaabbbccc