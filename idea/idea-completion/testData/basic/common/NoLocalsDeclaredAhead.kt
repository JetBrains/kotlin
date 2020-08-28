// FIR_COMPARISON
fun foo() {
    val v = {
        <caret>
        val hello = 1
        hello
    }
}
// ABSENT: hello