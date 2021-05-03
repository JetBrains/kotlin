// "Remove branch" "true"
fun test(x: Int): String {
    return when (x) {
        1 -> "1"
        2 -> "2"
        <caret>null -> "null"
        else -> ""
    }
}
/* IGNORE_FIR */
