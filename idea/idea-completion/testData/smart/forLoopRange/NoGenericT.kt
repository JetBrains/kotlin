fun<T> xxx1(): T {}
fun<T : Collection<String>> xxx2(): T {}

fun foo() {
    for (i in <caret>)
}

// ABSENT: xxx1
// EXIST: xxx2
