// FIR_IDENTICAL
fun someFun(i:Int) {
    val x = i ?: throw<!SYNTAX!><!>
}
