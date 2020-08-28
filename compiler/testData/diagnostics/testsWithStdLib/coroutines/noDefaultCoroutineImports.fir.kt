// FILE: 1.kt
// COMMON_COROUTINES_TEST

fun test(c: <!UNRESOLVED_REFERENCE!>Continuation<Unit><!>) {}

// FILE: 2.kt
import COROUTINES_PACKAGE.*

fun test2(c: Continuation<Unit>) {}
