// WITH_STDLIB

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

fun box(): String {
    return B().bar()
}
