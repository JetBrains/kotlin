// !LANGUAGE: +Coroutines
// SKIP_TXT
// !CHECK_TYPE

import kotlin.reflect.KSuspendFunction0

suspend fun foo() {}

fun test() {
    ::foo checkType { _<KSuspendFunction0>() }
}
