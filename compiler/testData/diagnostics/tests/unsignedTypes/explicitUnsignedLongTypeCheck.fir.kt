// !DIAGNOSTICS: -UNUSED_PARAMETER

val a0: Int = 1uL
val a1: UInt = 1uL
val a3: ULong = 1uL
val a4 = 1UL + 2UL
val a5 = <!UNRESOLVED_REFERENCE!>-<!>1UL

fun takeULong(u: ULong) {}

fun test() {
    takeULong(3UL)
    takeULong(1UL + 3uL)
    takeULong(1u + 0uL)
    takeULong(1uL + 4u)
}
