package bar

fun main(args : Array<String>) {
    class Some

    <!FUNCTION_CALL_EXPECTED!>Some<!>[<!SYNTAX!><!>] <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>names<!> <!DEBUG_INFO_MISSING_UNRESOLVED!><!SYNTAX!>=<!> ["ads"]<!>
}