// !WITH_NEW_INFERENCE
package a

class A {}

fun test(a1: A, a2: A) {
    val <!UNUSED_VARIABLE!>range<!> = "island".."isle"

    a1<!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>..<!>a2
}


public operator fun <T: Comparable<T>> T.rangeTo(<!UNUSED_PARAMETER!>that<!>: T): ClosedRange<T> {
    throw UnsupportedOperationException()
}