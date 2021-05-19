// FIR_IDENTICAL
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

package test.abc

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class E

@OptIn(test.abc.E::class)
fun f() {}

@test.abc.E
fun g() {}
