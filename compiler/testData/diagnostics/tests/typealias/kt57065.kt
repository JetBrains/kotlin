// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

typealias GlobalUndoLogRef = Long
fun GlobalUndoLogRef(p: Long): GlobalUndoLogRef = p

fun main() {
    GlobalUndoLogRef(42)
}
