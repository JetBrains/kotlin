// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KTIJ-15380

fun test() {
    var x = 0
    if (x == x) {}
}
