// !JVM_DEFAULT_MODE: all-compatibility
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
interface A {
    fun String.foo(k: String) = "O" + k
}

fun box(): String =
    object : A {
        fun box() = "FAIL".foo("K")
    }.box()
