// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_RUNTIME
// FULL_JDK
package test

inline fun <T, R> T.mylet(block: (T) -> R): R {
    return block(this)
}

// FILE: 2.kt
// NO_CHECK_LAMBDA_INLINING
import test.*

class Test {

    inline fun test() =
        mylet {
            {
                { "OK" }
            }()
        }
}

fun box(): String {
    val bar = Test().test()
    val clazz = bar::class.java

    if (clazz.enclosingClass == null) return "fail"

    return bar()
}