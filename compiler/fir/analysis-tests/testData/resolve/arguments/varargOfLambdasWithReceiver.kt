// ISSUE: KT-41991

fun runLambdas(vararg values: String.() -> Unit) {}

fun test() {
    runLambdas({
                   length
               })
}
