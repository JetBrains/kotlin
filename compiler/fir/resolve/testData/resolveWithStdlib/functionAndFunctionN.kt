fun takeAnyFun(function: Function<*>) {}

fun test(block: () -> Unit) {
    takeAnyFun(block)
}
