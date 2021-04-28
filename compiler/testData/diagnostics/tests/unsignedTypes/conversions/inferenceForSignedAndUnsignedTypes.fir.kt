// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE
// !CHECK_TYPE
// !WITH_NEW_INFERENCE

// Here we mostly trying to fix behaviour in order to track changes in inference rules for unsigned types later

fun <T> id(x: T): T = x
fun <K> select(x: K, y: K): K = TODO()

fun takeUByte(u: UByte) {}

fun foo() {
    <!NEW_INFERENCE_ERROR!>select(1, 1u)<!> checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Comparable<*>>() }
    takeUByte(<!ARGUMENT_TYPE_MISMATCH!>id(1)<!>)

    1 <!NONE_APPLICABLE!>+<!> 1u
    (1u <!NONE_APPLICABLE!>+<!> 1) <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!INAPPLICABLE_CANDIDATE!>_<!><UInt>() }

    id<UInt>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
}
