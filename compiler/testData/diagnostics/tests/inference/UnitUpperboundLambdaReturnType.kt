// FIR_IDENTICAL
// DIAGNOSTICS: -FINAL_UPPER_BOUND

fun <U : Unit> fork(task: () -> U) {}

fun test() {
    fork {}
    fork { Unit }
    fork { 42 }
}