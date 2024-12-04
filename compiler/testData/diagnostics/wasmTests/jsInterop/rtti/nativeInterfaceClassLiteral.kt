// FIR_IDENTICAL
external interface I

typealias TA = I
fun box() {
    println(<!EXTERNAL_INTERFACE_AS_CLASS_LITERAL!>I::class<!>)
    println(<!EXTERNAL_INTERFACE_AS_CLASS_LITERAL!>TA::class<!>)
}