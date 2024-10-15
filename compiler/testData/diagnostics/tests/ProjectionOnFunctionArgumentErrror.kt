// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun test() {
    fun <T> foo(){}
    foo<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>in<!> Int>()
}
