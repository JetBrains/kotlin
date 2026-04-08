// WITH_STDLIB

sealed class A : CharSequence {
    data class B(val c: CharSequence) : A(), CharSequence by c
}
