// "Change type arguments to <*, *>" "false"
// ACTION: Add 'map =' to argument
fun test(a: Any) {
    foo(a as Map<Int, Boolean><caret>)
}

fun foo(map: Map<Int, Boolean>) {}