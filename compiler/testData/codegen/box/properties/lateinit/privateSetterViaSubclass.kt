// JVM_ABI_K1_K2_DIFF: KT-63984
open class A {
    lateinit var x: String
        private set

    protected fun set(value: String) { x = value }
}

class B : A() {
    fun init() { set("OK") }
}

fun box(): String {
    val b = B()
    b.init()
    return b.x
}
