// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
class C {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    constructor() {}

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun f(){}

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    val p: String get() = ""
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(s: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
fun f(){}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
val p: String get() = ""
