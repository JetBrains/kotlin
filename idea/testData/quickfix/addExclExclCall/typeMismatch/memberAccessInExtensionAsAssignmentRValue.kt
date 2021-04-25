// "Add non-null asserted (!!) call" "true"
class C {
    val s: String? = null
}

// Test for KTIJ-10052
fun C.test() {
    val z: String = <caret>s
}
// TODO: Enable when FIR reports TYPE_MISMATCH for assignments
/* IGNORE_FIR */
