package a

class A {}

fun test(a1: A, a2: A) {
    val <!UNUSED_VARIABLE!>range<!> = "island".."isle"

    a1<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>..<!>a2
}


public fun <T: Comparable<T>> T.rangeTo(<!UNUSED_PARAMETER!>that<!>: T): Range<T> {
    throw UnsupportedOperationException()
}