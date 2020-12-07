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

interface B<R, T: B<List<R>, <!UPPER_BOUND_VIOLATED!>T<!>>> {
    fun r(): R
    fun t(): T
}

fun testB(b: B<*, *>) {
    <!TYPE_MISMATCH{OI}!>b<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>r<!>().<!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>checkType<!> { <!UNRESOLVED_REFERENCE{NI}!>_<!><Any?>() }
    <!TYPE_MISMATCH{OI}!>b<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>t<!>().<!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>checkType<!> { <!UNRESOLVED_REFERENCE{NI}!>_<!><B<List<*>, *>>() }

    <!TYPE_MISMATCH{OI}!><!TYPE_MISMATCH{OI}!>b<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>t<!>()<!>.<!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>r<!>().<!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>size<!>
}

