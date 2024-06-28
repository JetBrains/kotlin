// JVM_ABI_K1_K2_DIFF: KT-63984
interface A {
    val foo: String
}

class B : A {
    override var foo: String = "Fail"
        private set

    fun setOK(other: B) {
        other.foo = "OK"
    }
}

fun box(): String {
    val b = B()
    b.setOK(b)
    return b.foo
}
