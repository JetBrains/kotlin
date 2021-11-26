// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

interface A {
    fun foo(x: String = "OK"): String {
        return x
    }
}

fun box(): String {
    val x = object : A {}
    return x.foo()
}