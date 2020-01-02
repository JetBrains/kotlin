// !JVM_TARGET: 1.8
interface A {
    @JvmDefault
    fun test() {
    }
}

interface B{
    fun test() {
    }
}

interface AB: A, B

interface BA: B, A


interface C : A, B {
    @JvmDefault
    override fun test() {
        super<B>.test()
        super<A>.test()
    }
}

interface D : B, A {
    @JvmDefault
    override fun test() {
        super<B>.test()
        super<A>.test()
    }
}