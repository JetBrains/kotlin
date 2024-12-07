// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-41991

fun runLambdas(vararg values: String.() -> Unit) {}

fun test() {
    runLambdas({
                   length
               })
}
