// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
fun foo() {
    open class Local {
        val my: Int = 2
            get() = field
    }
    val your = object: Local() {
        val your: Int = 3
            get() = field
    }
}