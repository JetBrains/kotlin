// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class Outer<out E, in F> {
    inner class Inner {
        fun unsafe1(x: <!TYPE_VARIANCE_CONFLICT_ERROR!>E<!>) {}
        fun unsafe2(x: Collection<<!TYPE_VARIANCE_CONFLICT_ERROR!>E?<!>>) {}
        fun unsafe3(): <!TYPE_VARIANCE_CONFLICT_ERROR!>F?<!> = null
        fun unsafe4(): Collection<<!TYPE_VARIANCE_CONFLICT_ERROR!>F<!>>? = null
    }

    fun foo(x: <!TYPE_VARIANCE_CONFLICT_ERROR, TYPE_VARIANCE_CONFLICT_ERROR!>Inner<!>) {}
    fun bar(): Inner? = null
}
