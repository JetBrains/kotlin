// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

open class A {
    open fun ok(): String = "FAIL"
}

class B : A() {
    override fun ok(): String = "OK"
}

protocol interface Proto {
    fun foo(): A
}

class Impl {
    fun foo(): B = B()
}

fun box(): String {
    val proto: Proto = Impl()
    return proto.foo().ok()
}