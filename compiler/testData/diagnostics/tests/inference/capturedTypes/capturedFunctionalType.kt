// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-67212
fun withClues(vararg clues: () -> Any?) {
    arrayOf({ "" }, *clues)
}
