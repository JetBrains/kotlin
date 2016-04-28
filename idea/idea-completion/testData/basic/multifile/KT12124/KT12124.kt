val v = JavaClass().apply {
    foo = ""
    foo.<caret>
}

// EXIST: length
// EXIST: substring
