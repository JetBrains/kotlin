// IS_APPLICABLE: false

fun returnFun(fn: () -> Unit): (() -> Unit) -> Unit = {}

fun test() {
    returnFun {} ()<caret> {}
}