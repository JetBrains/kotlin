class A {
    fun test1a() = B().runnable1()
    fun test1b() = B().runnable1()
    fun test2a() = B().runnable2()
    fun test2b() = B().runnable2()

    fun testRunnableSamCtor1() = B().runnableSamCtor {}
    fun testRunnableSamCtor2() = B().runnableSamCtor {}
}

class B {
    inline fun runnable1() = Runnable {}
    inline fun runnable2() = Runnable {}

    inline fun runnableSamCtor(noinline s: () -> Unit ) = Runnable (s)
}
