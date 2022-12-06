// FIR_IDE_IGNORE
fun foo() {
    fun bar() = {
        <!INFERENCE_ERROR, INFERENCE_ERROR!>bar()<!>
    }
}
