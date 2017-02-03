// !API_VERSION: 1.0
// SKIP_TXT

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun foo(): Unit = <!UNRESOLVED_REFERENCE!>suspendCoroutine<!> {
    <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>resume<!>(Unit)
}

suspend fun bar(): Unit = <!UNRESOLVED_REFERENCE!>suspendCoroutineOrReturn<!> {
    <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>resume<!>(Unit)
    <!UNRESOLVED_REFERENCE!>COROUTINE_SUSPENDED<!>
}
