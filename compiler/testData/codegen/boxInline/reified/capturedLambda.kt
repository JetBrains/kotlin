// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <reified R> foo() = bar<R>() {"OK"}
inline fun <reified E> bar(crossinline y: () -> String) = {
    null is E
    run(y)
}

public inline fun <T> call(f: () -> T): T = f()

// FILE: 2.kt

import test.*

val x: () -> String = foo<String>()

fun box(): String {
    return x()
}
