val test1: (suspend () -> Unit)? = null
val test2: <!WRONG_MODIFIER_TARGET!>suspend<!> (() -> Unit)? = null
val test3: <!WRONG_MODIFIER_TARGET!>suspend<!> ( (() -> Unit)? ) = null

fun foo() {
    test1?.<!ILLEGAL_SUSPEND_FUNCTION_CALL!>invoke<!>()
    test2?.<!ILLEGAL_SUSPEND_FUNCTION_CALL!>invoke<!>()
    test3?.<!ILLEGAL_SUSPEND_FUNCTION_CALL!>invoke<!>()
}