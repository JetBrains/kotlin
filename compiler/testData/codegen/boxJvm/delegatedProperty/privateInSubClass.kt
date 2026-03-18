// TARGET_BACKEND: JVM
// WITH_STDLIB

open class A {
    private val _myVal by lazy {
        "1" + "2"
    }
}

class B : A() {
    private val _myVal by lazy {
        "O" + "K"
    }

    fun res() = _myVal
}

fun box(): String {
    return B().res()
}
