// +JDK

typealias Exn = java.lang.Exception

fun test() {
    throw <!FUNCTION_CALL_EXPECTED!>Exn<!>
}