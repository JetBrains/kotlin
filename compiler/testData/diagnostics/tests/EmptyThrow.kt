// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun someFun(i:Int) {
    val x = i ?: throw<!SYNTAX!><!>
}
