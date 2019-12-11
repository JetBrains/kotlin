annotation class A
annotation class A1(val x: Int)

annotation class B(
        val a: A = A(),
        val x: Int = A1(42).x,
        val aa: Array<A> = arrayOf(A())
)