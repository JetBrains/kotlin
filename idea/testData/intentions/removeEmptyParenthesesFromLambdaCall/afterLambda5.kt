// IS_APPLICABLE: false

fun <T> T.returnFun5(fn: (T) -> Boolean): ((T) -> T) -> Unit = {}

fun test() {
    25.returnFun5 { true } ()<caret> { it * 2 }
}