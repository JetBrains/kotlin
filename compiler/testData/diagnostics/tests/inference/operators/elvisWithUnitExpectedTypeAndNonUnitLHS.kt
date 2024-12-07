// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun unitFun() {}

fun myRun(x: () -> Unit) {}

fun nullStr(): String? = null

fun baz() {
    myRun {
        nullStr() ?: unitFun()
    }
}
