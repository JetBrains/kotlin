// ISSUE: KT-58282
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

@Deprecated(fun <!ANONYMOUS_FUNCTION_WITH_NAME!>box<!>() = <!DEPRECATION, FUNCTION_CALL_EXPECTED!>foo<!>)
<!NON_MEMBER_FUNCTION_NO_BODY!>fun foo()<!>
