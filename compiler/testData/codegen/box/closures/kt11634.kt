// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(): String
}

class AImpl(val z: String) : A {
    override fun foo(): String = z
}

open class AFabric {
    open fun createA(): A = AImpl("OK")
}

class AWrapperFabric : AFabric() {

    override fun createA(): A {
        return AImpl("fail")
    }

    fun createMyA(): A {
        return object : A by super.createA() {
        }
    }
}

fun box(): String {
    return AWrapperFabric().createMyA().foo()
}