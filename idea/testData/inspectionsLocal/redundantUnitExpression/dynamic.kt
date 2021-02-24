// PROBLEM: none
// ERROR: Unsupported [Dynamic types are not supported in this context]

fun foo() {
    fun bar(f: () -> dynamic): Unit {}
    bar {
        val a = 1
        Unit<caret>
    }
}