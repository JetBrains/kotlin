// "Add non-null asserted (!!) call" "true"

class SafeType {
    operator fun plus(arg: Int) {}
}

fun safeB(p: SafeType?) {
    val v = p <caret>+ 42
}
