// FILE: 1.kt

fun test(<!UNUSED_PARAMETER!>c<!>: <!UNRESOLVED_REFERENCE!>Continuation<!><Unit>) {}

// FILE: 2.kt
import kotlin.coroutines.experimental.*

fun test2(<!UNUSED_PARAMETER!>c<!>: Continuation<Unit>) {}
