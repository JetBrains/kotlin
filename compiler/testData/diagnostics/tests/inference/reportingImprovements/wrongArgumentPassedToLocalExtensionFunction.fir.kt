// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun Runnable.test(f: Runnable.(Int) -> Unit) {
    f(<!NO_VALUE_FOR_PARAMETER!>"")<!>
}

fun test(f: Runnable.(Int) -> Unit, runnable: Runnable) {
    with (runnable) {
        f(<!NO_VALUE_FOR_PARAMETER!>"")<!>
    }
}

fun Int.test(f: String.(Int) -> Unit) {
    f("", 0)
    f(<!NO_VALUE_FOR_PARAMETER!>"")<!>
    with("") {
        f(0)
        f(<!NO_VALUE_FOR_PARAMETER!>0.0)<!>
    }
}
