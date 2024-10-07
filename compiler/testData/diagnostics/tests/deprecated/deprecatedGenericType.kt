// FIR_IDENTICAL
@Deprecated("")
class DeprecatedClassWithParameter<T>

fun foo(
    d1: <!DEPRECATION!>DeprecatedClassWithParameter<!><List<Int>>,
    d2: List<<!DEPRECATION!>DeprecatedClassWithParameter<!><Int>>
) {}
