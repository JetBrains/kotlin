// WITH_RUNTIME
// PROBLEM: none

class J {
    @JvmField
    val <caret>b = ""

    fun foo() {
        println(b)
    }
}