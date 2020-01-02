// !JVM_TARGET: 1.8

abstract class A {

    @JvmDefault
    fun test() {}

    @JvmDefault
    abstract fun test2(s: String = "")

    @JvmDefault
    abstract fun test3()
}

object B {

    @JvmDefault
    fun test() {}

    @JvmDefault
    fun test2(s: String = "") {}
}
