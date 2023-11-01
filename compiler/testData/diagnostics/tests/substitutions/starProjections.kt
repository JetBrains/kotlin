// ISSUE: KT-63072
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
    b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>r<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Any?>() }
    b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>t<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><B<List<*>, *>>() }

    b.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>t<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>r<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>size<!>
}
