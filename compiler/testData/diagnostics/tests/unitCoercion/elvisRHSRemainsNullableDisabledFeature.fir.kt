// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ElvisInferenceImprovementsIn21
// ISSUE: KT-71751

fun <R> myRun(x: () -> R): R = TODO()
fun launch(x: () -> Unit) {}
fun baz() {}

fun main() {
    launch {
        // We should not run Unit-coercion for `myRun { null }`
        myRun { null } <!USELESS_ELVIS!>?: baz()<!>
    }
}
