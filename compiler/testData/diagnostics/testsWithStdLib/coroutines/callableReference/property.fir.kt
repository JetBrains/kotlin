// SKIP_TXT
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.coroutineContext

val c = ::coroutineContext

fun test() {
    c()
}

suspend fun test2() {
    c()
}