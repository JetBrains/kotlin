// "Add non-null asserted (!!) call" "true"

class SafeType {
    operator fun unaryMinus() {}
}

fun safeB(p: SafeType?) {
    val v = <caret>-p
}
/* IGNORE_FIR */