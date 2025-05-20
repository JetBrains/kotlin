// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// CHECK_TYPE

// Here we mostly trying to fix behaviour in order to track changes in inference rules for unsigned types later

fun <T> id(x: T): T = x
fun <K> select(x: K, y: K): K = TODO()

fun takeUByte(u: UByte) {}

fun foo() {
    <!TYPE_MISMATCH("UInt; Int")!>select(1, 1u)<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Comparable<*>>() }
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>id(1)<!>)

    1 + <!ARGUMENT_TYPE_MISMATCH!>1u<!>
    (1u + <!ARGUMENT_TYPE_MISMATCH!>1<!>) checkType { _<UInt>() }

    id<UInt>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
}
