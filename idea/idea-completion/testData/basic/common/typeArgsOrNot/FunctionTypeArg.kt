fun foo() {
    val v = listOf<<caret>
}

// EXIST: String
// EXIST: kotlin
// ABSENT: defaultBufferSize
// ABSENT: readLine
