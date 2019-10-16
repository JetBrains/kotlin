// "Change type arguments to <*>" "false"
fun test(a: Any) {
    (a as List<Boolean><caret>).bar()
}

fun List<Boolean>.bar() {}