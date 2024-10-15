// RUN_PIPELINE_TILL: SOURCE
// +JDK

typealias Exn = java.lang.Exception

fun test() {
    throw <!NO_COMPANION_OBJECT, TYPE_MISMATCH!>Exn<!>
}
