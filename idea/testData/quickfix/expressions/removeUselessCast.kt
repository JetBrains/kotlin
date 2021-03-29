// "Remove useless cast" "true"
fun foo(a: String) {
    val b = a <caret>as String
}

/* IGNORE_FIR */