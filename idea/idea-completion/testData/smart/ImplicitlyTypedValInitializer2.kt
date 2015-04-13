class A(s: String) {
    val v = s.<caret>length()
}

// EXIST: size
// ABSENT: substring