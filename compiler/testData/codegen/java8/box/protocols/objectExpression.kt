// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Proto {
    fun foo(): String
}

fun box(): String = object : Proto {
    override fun foo(): String = "OK"
}.foo()
