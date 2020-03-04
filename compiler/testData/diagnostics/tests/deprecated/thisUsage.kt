// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

@Deprecated("Use A instead") open class MyClass {
    fun foo() {
        val test = this
    }
}
