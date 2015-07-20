fun foo(list: List<String>): Collection<Int> {
    bar(list.map { it.<caret> })
}

fun bar(p: Collection<Int>) {
}

fun bar(p: Collection<String>, b: Boolean) {
}

// EXIST: length
// EXIST: substring
// ABSENT: isEmpty
