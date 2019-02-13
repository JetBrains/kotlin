// "Change type arguments to <*, *>" "false"
// ACTION: Convert to run
// ACTION: Convert to with
fun test(a: Any) {
    (a as Map<Int, Boolean><caret>).bar()
}

fun Map<Int, Boolean>.bar() {}