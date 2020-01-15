object ForceSam : java.util.Comparator<Runnable> {
    override fun compare(o1: Runnable, o2: Runnable): Int = 0
}

fun test(r: Runnable) {
    ForceSam.compare(r, r)
    ForceSam.compare({}, {})

    ForceSam.compare(r, {})
    ForceSam.compare({}, r)
}

// Check that new inference is enabled
object Scope {
    interface A
    interface B<T>
    class C<T>

    fun <T, K> foo(<warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: C<T>) where K : A, K : B<T> {}

    fun usage(c: C<Any>) {
        foo(c) // should compile only in NI
    }
}
