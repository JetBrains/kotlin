// !DIAGNOSTICS: -UNUSED_PARAMETER
class Outer<out E, in F> {
    inner class Inner {
        fun unsafe1(x: <!TYPE_VARIANCE_CONFLICT!>E<!>) {}
        fun unsafe2(x: Collection<<!TYPE_VARIANCE_CONFLICT!>E?<!>>) {}
        fun unsafe3(): <!TYPE_VARIANCE_CONFLICT!>F?<!> = null
        fun unsafe4(): Collection<<!TYPE_VARIANCE_CONFLICT!>F<!>>? = null
    }

    // Should be errors
    // Refinement of variance checker is needed
    fun foo(x: Inner) {}
    fun bar(): Inner? = null
}
