// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
}

class Impl {
    fun foo(): String = "OK"
}

fun id(x: Proto): Proto = x

fun box(): String {
    val obj = Impl()
    val proto = id(obj)
    return proto.foo()
}
