// "Add non-null asserted (!!) call" "true"
fun test() {
    val s: String? = null
    val z: String = <caret>s
}
// TODO: Enable when FIR reports TYPE_MISMATCH for assignments
/* IGNORE_FIR */
