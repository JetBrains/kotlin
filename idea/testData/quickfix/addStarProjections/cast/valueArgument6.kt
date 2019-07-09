// "Change type arguments to <*>" "false"
// ACTION: Add 'list =' to argument
fun test(a: Any) {
    foo(a as? List<Boolean><caret>)
}

fun foo(list: List<Boolean>?) {}