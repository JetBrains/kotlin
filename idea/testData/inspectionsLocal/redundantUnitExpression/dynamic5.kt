// ERROR: Unsupported [Dynamic types are not supported in this context]

fun foo() {
    fun <T> bar(c: () -> T, f: () -> dynamic): Unit {}
    bar({
            val a = 1
            Unit<caret>
        }) {
        val a = 1
        Unit
    }
}