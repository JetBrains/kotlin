// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    inline fun() {}
    tailrec fun() {}
    operator fun() {}
    external fun() {}
    infix fun() {}
    <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {}
    <!WRONG_MODIFIER_TARGET!>expect<!> fun() {}
    <!WRONG_MODIFIER_TARGET!>actual<!> fun() {}
    <!WRONG_MODIFIER_TARGET!>override<!> fun() {}
}