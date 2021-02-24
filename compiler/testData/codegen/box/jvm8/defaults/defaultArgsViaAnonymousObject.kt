// !JVM_DEFAULT_MODE: enable
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface A {
    @JvmDefault
    fun foo(x: String = "OK"): String {
        return x
    }
}

fun box(): String {
    val x = object : A {}
    return x.foo()
}