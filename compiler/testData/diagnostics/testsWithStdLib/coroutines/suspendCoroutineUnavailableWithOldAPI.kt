// !API_VERSION: 1.0
// SKIP_TXT

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

<!UNSUPPORTED!>suspend<!> fun foo(): Unit = <!UNRESOLVED_REFERENCE!>suspendCoroutine<!> {
    <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>resume<!>(Unit)
}

<!UNSUPPORTED!>suspend<!> fun bar(): Unit = <!UNRESOLVED_REFERENCE!>suspendCoroutineUninterceptedOrReturn<!> {
    <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>resume<!>(Unit)
    <!UNRESOLVED_REFERENCE!>COROUTINE_SUSPENDED<!>
}
