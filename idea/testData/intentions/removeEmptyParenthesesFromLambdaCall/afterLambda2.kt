// IS_APPLICABLE: false

fun returnFun(fn: () -> Unit, i: Int): (() -> Unit) -> Unit = {}

fun test() {
    returnFun({}, 1)()<caret> {}
}