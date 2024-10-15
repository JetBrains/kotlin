// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
object Host {
    val `____` = { -> }
    fun testFunTypeVal() {
        <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>()
    }
}
