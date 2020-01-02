// !CHECK_TYPE
// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

import kotlin.reflect.KSuspendFunction0

suspend fun foo() {}

fun test() {
    ::foo checkType { <!UNRESOLVED_REFERENCE!>_<!><KSuspendFunction0<Unit>>() }
}
