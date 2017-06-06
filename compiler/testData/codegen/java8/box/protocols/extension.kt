// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
}

class Impl {
    fun foo(): String = "K"
}

fun Proto.ofoo(): String {
    return "O" + foo()
}

fun box(): String {
    val proto: Proto = Impl()
    return proto.ofoo()
}