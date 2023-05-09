// !DIAGNOSTICS: -UNUSED_PARAMETER

fun fail1(c: suspend () -> Unit) {}

fun fail2(c: () -> Unit) {}

fun success1(c: suspend () -> Unit) {}

fun test1() {
    fail1(<!ARGUMENT_TYPE_MISMATCH!>fun () {}<!>)
    fun fail2(c: suspend () -> Unit) {}
    fail2(fun () {})
    fun success1(c: () -> Unit) {}
    success1(fun() {})
}

suspend fun fail3(c: suspend () -> Unit) {}

suspend fun fail4(c: () -> Unit) {}

suspend fun success2(c: suspend () -> Unit) {}

suspend fun test2() {
    fail3(<!ARGUMENT_TYPE_MISMATCH!>fun () {}<!>)
    fun fail4(c: suspend () -> Unit) {}
    fail4(fun () {})
    fun success2(c: () -> Unit) {}
    success2(fun() {})
}
