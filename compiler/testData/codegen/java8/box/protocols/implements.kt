// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
}

class A : Proto {
    override fun foo(): String = "OK"
}

fun box(): String = A().foo()