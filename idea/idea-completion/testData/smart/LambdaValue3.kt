fun foo(list: List<String>) {
    bar(list.map { it.<caret> })
}

fun bar(p: Collection<Int>) {
}

fun bar(p: Collection<String>, b: Boolean) {
}

// EXIST: length
// EXIST: substring
// ABSENT: isEmpty
