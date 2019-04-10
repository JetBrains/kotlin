// PROBLEM: none
// WITH_RUNTIME
fun test(b: Boolean) {
    <caret>if (b) throw IndexOutOfBoundsException()
}
