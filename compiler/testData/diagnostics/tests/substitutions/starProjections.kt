// !CHECK_TYPE

trait A<R, T: A<R, T>> {
    fun r(): R
    fun t(): T
}

fun testA(a: A<*, *>) {
    a.r().checkType { it : _<Any?> }
    a.t().checkType { it : _<A<*, *>> }
}

trait B<R, T: B<List<R>, <!UPPER_BOUND_VIOLATED!>T<!>>> {
    fun r(): R
    fun t(): T
}

fun testB(b: B<*, *>) {
    b.r().checkType { it : _<Any?> }
    b.t().checkType { it : _<B<List<*>, *>> }

    b.t().r().size()
}

