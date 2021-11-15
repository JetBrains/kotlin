// TARGET_BACKEND: JVM
// WITH_STDLIB

open class A {
    @JvmOverloads
    fun foo(x: String, y: String = "", z: String = "K"): String {
        return x + y + z
    }
}

class B : A()

fun box(): String {
    return B().foo("O")
}
