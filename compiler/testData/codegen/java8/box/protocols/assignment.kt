// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun x(): String
}

class Foo {
    fun x(): String = "OK"
}

fun box(): String {
    val impl: Proto = Foo()
    return impl.x()
}
