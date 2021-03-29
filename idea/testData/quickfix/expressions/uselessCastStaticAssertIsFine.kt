// "Remove useless cast" "true"
fun foo(a: Any) {
    val b = a <caret>as Any
}

/* IGNORE_FIR */