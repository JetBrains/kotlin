// RUN_PIPELINE_TILL: BACKEND
fun takeAnyFun(function: Function<*>) {}

fun test(block: () -> Unit) {
    takeAnyFun(block)
}
