// RUN_PIPELINE_TILL: FRONTEND
fun test() {
    <!NOT_YET_SUPPORTED_IN_INLINE!>inline<!> fun() {}
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun() {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun() {}
    <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external fun()<!> {}
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun() {}
    suspend fun() {}
    <!WRONG_MODIFIER_TARGET!>expect<!> fun() {}
    <!WRONG_MODIFIER_TARGET!>actual<!> fun() {}
    <!WRONG_MODIFIER_TARGET!>override<!> fun() {}
}