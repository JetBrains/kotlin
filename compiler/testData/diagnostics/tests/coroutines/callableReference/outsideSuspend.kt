// !CHECK_TYPE
// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

import kotlin.reflect.KSuspendFunction0

suspend fun foo() {}

fun test() {
    ::foo checkType { _<KSuspendFunction0<Unit>>() }
}
