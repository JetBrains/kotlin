// "Add non-null asserted (!!) call" "true"

class SafeType {
    infix fun op(arg: Int) {}
}

fun safeB(p: SafeType?) {
    val v = p <caret>op 42
}
/* IGNORE_FIR */