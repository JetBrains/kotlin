// IS_APPLICABLE: false

fun returnFun2(i: Int): (() -> Unit) -> Unit = {}

fun test22() {
    returnFun2(1)()<caret> {}
}