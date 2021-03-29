// "Remove useless elvis operator" "true"
fun foo(a: String) {
    val b : String = a <caret>?: "s"
}

/* IGNORE_FIR */