// !WITH_NEW_INFERENCE
// !CHECK_TYPE

interface A<R, T: A<R, T>> {
    fun r(): R
    fun t(): T
}

fun testA(a: A<*, *>) {
    a.r().checkType { _<Any?>() }
    a.t().checkType { _<A<*, *>>() }
}

interface B<R, T: B<List<R>, T>> {
    fun r(): R
    fun t(): T
}

fun testB(b: B<*, *>) {
    b.r().checkType { _<Any?>() }
    b.t().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><B<List<*>, *>>() }

    b.t().r().size
}

