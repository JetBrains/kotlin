object ForceSam : java.util.Comparator<Runnable> {
    override fun compare(o1: Runnable, o2: Runnable): Int = 0
}

fun test(r: Runnable) {
    ForceSam.compare(r, r)
    ForceSam.compare({}, {})

    ForceSam.compare(r, <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is () -> Unit but Runnable was expected">{}</error>)
    ForceSam.compare(<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is () -> Unit but Runnable was expected">{}</error>, r)
}

// Check that new inference is enabled
object Scope {
    interface A
    interface B<T>
    class C<T>

    fun <T, K> foo(<warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: C<T>) where K : A, K : B<T> {}

    fun usage(c: C<Any>) {
        <error descr="[TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS] Type inference failed: Cannot infer type parameter K in fun <T, K : Scope.A> foo(c: Scope.C<T>): Unit where K : Scope.B<T>
None of the following substitutions
(Scope.C<Any>)
(Scope.C<Any>)
can be applied to
(Scope.C<Any>)
">foo</error>(c) // should compile only in NI
    }
}
