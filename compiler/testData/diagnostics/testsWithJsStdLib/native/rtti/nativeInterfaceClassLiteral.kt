// FIR_IDENTICAL
external interface I

fun box() {
    println(<!EXTERNAL_INTERFACE_AS_CLASS_LITERAL!>I::class<!>)
}