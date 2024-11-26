// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71704

fun testIt(l: List<Int>) {
    l.flatMap {
        f -> {}
    }
}
