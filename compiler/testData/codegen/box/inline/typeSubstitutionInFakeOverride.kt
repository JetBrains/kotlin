// WITH_STDLIB

// FILE: lib.kt
import kotlin.test.*

abstract class A {
    inline fun <reified T : Any> baz(): String {
        return T::class.simpleName!!
    }
}

class B : A() {
    fun bar(): String {
        return baz<OK>()
    }
}

class OK

// FILE: main.kt
fun box(): String {
    return B().bar()
}
