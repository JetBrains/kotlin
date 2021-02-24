// WITH_REFLECT
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <reified R, T> bar(crossinline tasksFactory: () -> T) = {
    null is R
    run(tasksFactory)
}

public inline fun <T> call(f: () -> T): T = f()

// FILE: 2.kt

import test.*

inline fun <reified R> foo() = bar<R, String>() {"OK"}

fun box(): String {
    return foo<String>()()
}
