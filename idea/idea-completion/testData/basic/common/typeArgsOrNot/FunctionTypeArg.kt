fun foo() {
    val v = listOf<<caret>
}

// EXIST: String
// EXIST: java
// ABSENT: defaultBufferSize
// ABSENT: readLine
