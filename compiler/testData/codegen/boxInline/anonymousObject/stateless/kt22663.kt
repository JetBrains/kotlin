// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_RUNTIME
// FULL_JDK
package test

inline fun <T> myrun(block: () -> T): T {
    return block()
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING

import test.*

var result = "fail"


class Foo {
    fun bar(obj: String) =
        myrun {
            {
                result = obj;
                val z = "K"
                { "K" }
            } ()
        }
    }

fun box(): String {
    val bar = Foo().bar("OK")
    val clazz = bar::class.java

    if (clazz.enclosingClass == null) return "fail"

    return result
}
