fun String?.dot(): Nothing {
    throw RuntimeException()
}

fun box(): String {
    val s: String? = ""
    return s.<caret>
}

// EXIST: dot