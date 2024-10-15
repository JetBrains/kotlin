// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

package test.abc

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class E

@OptIn(test.abc.E::class)
fun f() {}

@test.abc.E
fun g() {}
