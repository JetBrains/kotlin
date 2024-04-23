// ISSUE: KT-67688
// DIAGNOSTICS: -FINAL_UPPER_BOUND

fun <U : Unit> fork(task: () -> U) {}

fun bar() {}

fun test(x: Boolean) {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fork<!> {
        return@fork Unit
    }

    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>fork<!> {
        if (x)
            return@fork Unit
        else
            bar()
    }
}
