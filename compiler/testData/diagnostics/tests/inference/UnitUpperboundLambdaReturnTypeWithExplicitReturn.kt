// FIR_IDENTICAL
// ISSUE: KT-67688
// DIAGNOSTICS: -FINAL_UPPER_BOUND

fun <U : Unit> fork(task: () -> U) {}

fun bar() {}

fun test(x: Boolean) {
    fork {
        return@fork Unit
    }

    fork {
        if (x)
            return@fork Unit
        else
            bar()
    }
}
