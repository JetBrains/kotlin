// !DIAGNOSTICS: -UNUSED_PARAMETER

fun unreachable0() {
    return
    <!UNREACHABLE_CODE!>return todo()<!>
}

fun unreachable2() {
    return
    <!UNREACHABLE_CODE!>val a = todo()<!>
}

fun unreachable3() {
    return
    <!UNREACHABLE_CODE!>bar(todo())<!>
}

fun unreachable4(array: Array<Any>) {
    return
    <!UNREACHABLE_CODE!>array[todo()]<!>
}

fun bar(a: Any) {}
fun todo(): Nothing = throw Exception()
