// !DIAGNOSTICS: -UNUSED_PARAMETER

val a0: Int = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1uL<!>
val a1: UInt = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1uL<!>
val a3: ULong = 1uL
val a4 = 1UL + 2UL
val a5 = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>-<!>1UL

fun takeULong(u: ULong) {}

fun test() {
    takeULong(3UL)
    takeULong(1UL + 3uL)
    takeULong(1u + 0uL)
    takeULong(1uL + 4u)
}