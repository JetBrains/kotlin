// FIR_COMPARISON
fun usage() {
    val a = "10".toIntOrNull()

    a?.<caret>
}

// EXIST: toString