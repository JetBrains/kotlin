// !CHECK_TYPE
// !LANGUAGE: +ReleaseCoroutines
// !API_VERSION: 1.3
// SKIP_TXT

import kotlin.reflect.KSuspendFunction0

suspend fun foo() {}

fun test() {
    ::foo checkType { _<KSuspendFunction0<Unit>>() }
}
