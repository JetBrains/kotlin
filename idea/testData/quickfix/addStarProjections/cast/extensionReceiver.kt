// "Change type arguments to <*>" "false"
// ACTION: Convert to run
// ACTION: Convert to with
fun test(a: Any) {
    (a as List<Boolean><caret>).bar()
}

fun List<Boolean>.bar() {}