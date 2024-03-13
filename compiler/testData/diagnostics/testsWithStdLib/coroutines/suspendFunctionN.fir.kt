// !LANGUAGE: +Coroutines
// !DIAGNOSTICS: -USELESS_IS_CHECK
// SKIP_TXT

fun test() {
    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {} is <!UNRESOLVED_REFERENCE!>SuspendFunction0<!><*>
    <!NON_MODIFIER_FORM_FOR_BUILT_IN_SUSPEND!>suspend<!> {} is kotlin.coroutines.SuspendFunction0<*>
}
