fun <T> Some<T>.close() {
}

class Some<T>() {
}

fun test() {
    val s = Some<String>()
    s.<caret>
}

// EXIST: close