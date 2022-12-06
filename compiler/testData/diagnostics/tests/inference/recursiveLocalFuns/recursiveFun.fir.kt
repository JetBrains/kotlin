// FIR_IDE_IGNORE
fun foo() {
    fun bar() = (fun() = <!INFERENCE_ERROR!>bar()<!>)
}
