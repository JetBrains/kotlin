// ISSUE: KT-90080

annotation class A1(val x: Int = 0)
annotation class A2(val a: A1 = A1(42))
annotation class A3(val a2: A2 = A2(A1(1)))
annotation class AA(val xs: Array<A1> = [A1(1), A1(2)])
annotation class AV(vararg val xs: A1 = [A1(3)])

@A2 @A3 @AA @AV
fun test1() {}

@A2(A1()) @A3(A2(A1(2))) @AA([A1(3)]) @AV(A1(4), A1(5))
fun test2() {}
