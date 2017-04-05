// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Prototo {
    fun x(): String
}

class X {
    fun x(): String = "OK"
}

fun box(): String {
    val x: Prototo = X()
    val y: X = x as X
    return y.x()
}
