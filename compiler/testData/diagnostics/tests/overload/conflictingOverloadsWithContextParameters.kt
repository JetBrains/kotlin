// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

open class C {
    fun <T> some(s: String): T = null!!

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun <T> some2(text: String): T = null!!

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun <T> some3(text: String): T = null!!
}

open class X5 : C() {
    <!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun some(text: String): String<!> = ""

    <!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun some2(text: String): String<!> = ""

    <!CONFLICTING_OVERLOADS!><!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
    fun some3(text: String): String<!> = ""
}
