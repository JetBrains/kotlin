// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun dyn(d: dynamic) {}

fun foo(d: dynamic): String = ""
fun foo(d: Int): Int = 1

fun nothing(d: dynamic): Int = 1
fun nothing(d: Nothing): String = ""

fun test(d: dynamic) {
    dyn(1)
    dyn("")

    foo(1).<!UNRESOLVED_REFERENCE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    foo("").<!UNRESOLVED_REFERENCE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    // Checking specificity of `dynamic` vs `Nothing`
    nothing(d).<!UNRESOLVED_REFERENCE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    nothing("").<!UNRESOLVED_REFERENCE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    @Suppress("UNREACHABLE_CODE") nothing(null!!).<!UNRESOLVED_REFERENCE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
}
