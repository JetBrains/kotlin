// ISSUE: KT-66611
// JVM_ABI_K1_K2_DIFF

annotation class A(vararg val xs: String)
annotation class B(vararg val xa: A)
annotation class C(vararg val xc: C)

@A(*arrayOf("O"), "K")
@B(*arrayOf(A("O", *arrayOf("K"))), A())
@C(*arrayOf(C()))
fun box() = "OK"
