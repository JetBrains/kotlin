// !DIAGNOSTICS: -UNUSED_PARAMETER
class Outer<out E, in F> {
    inner class Inner {
        fun unsafe1(x: E) {}
        fun unsafe2(x: Collection<E?>) {}
        fun unsafe3(): F? = null
        fun unsafe4(): Collection<F>? = null
    }

    // Should be errors
    // Refinement of variance checker is needed
    fun foo(x: Inner) {}
    fun bar(): Inner? = null
}
