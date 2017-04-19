// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Master {
    fun foo(x: String) = x
}

fun box(): String {
    val x: Master = object : Master {}
    return x.foo("OK")
}
