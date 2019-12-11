// !DIAGNOSTICS: -UNUSED_PARAMETER

fun unreachable0() {
    return
    return todo()
}

fun unreachable2() {
    return
    val a = todo()
}

fun unreachable3() {
    return
    bar(todo())
}

fun unreachable4(array: Array<Any>) {
    return
    array[todo()]
}

fun bar(a: Any) {}
fun todo(): Nothing = throw Exception()
