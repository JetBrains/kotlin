// !USE_EXPERIMENTAL: kotlin.Experimental

package test.abc

@Experimental
annotation class E

@UseExperimental(test.abc.E::class)
fun f() {}

@test.abc.E
fun g() {}
