fun foo(list: List<String>) {
    list.filter { it.<caret> }
}

// EXIST: isEmpty
// EXIST: isBlank
// ABSENT: substring
