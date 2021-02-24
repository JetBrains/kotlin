// !DIAGNOSTICS: -UNUSED_VARIABLE

package mpp

fun jsFun() {
}

fun test() {
    val string: String = ""
    val ref: kotlin.reflect.KCallable<*> = ::jsFun
    ref.name
    // should be unresolved
    ref.<!UNRESOLVED_REFERENCE!>call<!>()
}