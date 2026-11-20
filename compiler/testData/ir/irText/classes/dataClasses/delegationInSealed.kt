// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib tests compile against JDK 17 instead of the mock JDK.

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}
