// IGNORE_BACKEND: JVM_IR
interface A {
    fun foo(): String
}

class AImpl(val z: String) : A {
    override fun foo(): String = z
}

open class AFabric {
    open fun createA(z: String): A = AImpl(z)
}

class AWrapperFabric : AFabric() {

    override fun createA(z: String): A {
        return AImpl("fail: $z")
    }

    fun createMyA(): A {
        val z = "OK"
        return object : A by super.createA(z) {}
    }
}

fun box(): String {
    return AWrapperFabric().createMyA().foo()
}