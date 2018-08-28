// SKIP_TXT
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.coroutineContext

val c = ::<!UNSUPPORTED!>coroutineContext<!>

fun test() {
    c()
}

suspend fun test2() {
    c()
}