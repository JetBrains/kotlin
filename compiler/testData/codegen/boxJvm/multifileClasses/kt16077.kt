// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:JvmMultifileClass

class A {
    private var r: String = "fail"
    public fun getR(): String = "OK"
}

fun box() = A().getR()