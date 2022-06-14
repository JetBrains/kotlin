// WITH_STDLIB
// SKIP_KLIB_TEST

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}
