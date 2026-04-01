// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@file:OptIn(ExperimentalStdlibApi::class)

import java.io.IOException

class Foo {
    @JvmExposeBoxed
    @Throws(IOException::class)
    fun foo(i: UInt) {}
}

fun box(): String {
    val method = Foo::class.java.declaredMethods.single { it.name == "foo" }

    if (method.exceptionTypes.contains(IOException::class.java)) {
        return "OK"
    }
    return "FAIL $method"
}
