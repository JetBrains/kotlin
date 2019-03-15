// !DIAGNOSTICS: -UNUSED_PARAMETER

fun fail1(c: suspend () -> Unit) {}

fun fail2(c: () -> Unit) {}

fun success1(c: suspend () -> Unit) {}

fun test1() {
    fail1(<!TYPE_MISMATCH!>fun () {}<!>)
    fun fail2(c: suspend () -> Unit) {}
    fail2(<!TYPE_MISMATCH!>fun () {}<!>)
    fun success1(c: () -> Unit) {}
    success1(fun() {})
}

suspend fun fail3(c: suspend () -> Unit) {}

suspend fun fail4(c: () -> Unit) {}

suspend fun success2(c: suspend () -> Unit) {}

suspend fun test2() {
    fail3(<!TYPE_MISMATCH!>fun () {}<!>)
    fun fail4(c: suspend () -> Unit) {}
    fail4(<!TYPE_MISMATCH!>fun () {}<!>)
    fun success2(c: () -> Unit) {}
    success2(fun() {})
}