// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

import kotlin.coroutines.experimental.coroutineContext

suspend fun test() {
    <!UNSUPPORTED!>coroutineContext<!>
}
