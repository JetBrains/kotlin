// FIR_IDENTICAL
// ISSUE: KT-67212
fun withClues(vararg clues: () -> Any?) {
    arrayOf({ "" }, *clues)
}
