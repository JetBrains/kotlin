// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface IFoo {
    fun foo()
}

fun outer() {
    fun IFoo.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() {}
}