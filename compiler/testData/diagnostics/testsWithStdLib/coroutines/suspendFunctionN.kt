// FIR_IDENTICAL
// !LANGUAGE: +Coroutines
// !DIAGNOSTICS: -USELESS_IS_CHECK
// SKIP_TXT

fun test() {
    suspend {} is <!UNRESOLVED_REFERENCE!>SuspendFunction0<!><*>
    suspend {} is kotlin.coroutines.SuspendFunction0<*>
}
