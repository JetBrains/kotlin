package bar

fun main(args : Array<String>) {
    class Some

    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!NO_DEFAULT_OBJECT!>Some<!>[<!SYNTAX!><!>]<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>names<!> <!DEBUG_INFO_MISSING_UNRESOLVED!><!SYNTAX!>=<!> ["ads"]<!>
}