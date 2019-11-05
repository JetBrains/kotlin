// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
interface A {
    @JvmDefault
    fun String.foo() = "OK"
}

fun box(): String =
    object : A { fun box() = "FAIL".foo() }.box()
