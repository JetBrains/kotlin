// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// CHECK_TYPE

// Here we mostly trying to fix behaviour in order to track changes in inference rules for unsigned types later

fun <T> id(x: T): T = x
fun <K> select(x: K, y: K): K = TODO()

fun takeUByte(u: UByte) {}

fun foo() {
    <!TYPE_MISMATCH!>select<!>(1, 1u) checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Comparable<*>>() }
    takeUByte(<!TYPE_MISMATCH!>id(1)<!>)

    1 <!NONE_APPLICABLE!>+<!> 1u
    (1u + <!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>) checkType { _<UInt>() }

    id<UInt>(<!SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED!>1<!>)
}
