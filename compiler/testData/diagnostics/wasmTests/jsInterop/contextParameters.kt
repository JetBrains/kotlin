// DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// LANGUAGE: +ContextParameters

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
fun d(): Unit = js("console.log(x)")

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
@JsFun("console.log(x)")
external fun d2(): Unit

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
external fun d3(): Unit

external class E {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
    fun d4(): Unit
}
