// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

package test.abc

@RequiresOptIn
annotation class E

@OptIn(test.abc.E::class)
fun f() {}

@test.abc.E
fun g() {}
