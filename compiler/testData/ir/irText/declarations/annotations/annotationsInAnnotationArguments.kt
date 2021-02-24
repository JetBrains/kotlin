// FIR_IDENTICAL
annotation class A1(val x: Int)
annotation class A2(val a: A1)
annotation class AA(val xs: Array<A1>)

@A2(A1(42))
@AA([A1(1), A1(2)])
fun test() {}
