// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
object Host {
    val `____` = { -> }
    fun testFunTypeVal() {
        <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>____<!>()
    }
}
