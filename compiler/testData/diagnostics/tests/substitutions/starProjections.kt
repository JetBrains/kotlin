// !CHECK_TYPE

interface A<R, T: A<R, T>> {
    fun r(): R
    fun t(): T
}

fun testA(a: A<*, *>) {
    a.r().checkType { _<Any?>() }
    a.t().checkType { _<A<*, *>>() }
}

interface B<R, T: B<List<R>, <!UPPER_BOUND_VIOLATED!>T<!>>> {
    fun r(): R
    fun t(): T
}

fun testB(b: B<*, *>) {
    <!TYPE_MISMATCH(B<out Any?, out B<List<*>, *>>; B<*, *>)!>b<!>.r().checkType { _<Any?>() }
    <!TYPE_MISMATCH(B<out Any?, out B<List<*>, *>>; B<*, *>)!>b<!>.t().checkType { _<B<List<*>, *>>() }

    <!TYPE_MISMATCH(B<List<Any?>, out B<List<*>, *>>; B<List<*>, *>)!><!TYPE_MISMATCH(B<out Any?, out B<List<*>, *>>; B<*, *>)!>b<!>.t()<!>.r().size
}

