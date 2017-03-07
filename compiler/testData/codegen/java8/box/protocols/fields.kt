// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    val x: String
}

class A {
    val x: String = "OK"
}

fun test(x: Proto): String = x.x
fun box(): String = test(A())
