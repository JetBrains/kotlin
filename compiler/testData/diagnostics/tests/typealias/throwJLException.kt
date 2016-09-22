// +JDK

typealias Exn = java.lang.Exception

fun test() {
    throw <!NO_COMPANION_OBJECT!>Exn<!>
}