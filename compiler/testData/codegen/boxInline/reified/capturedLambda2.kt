// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// WITH_REFLECT
package test

inline fun <reified R, T> bar(crossinline tasksFactory: () -> T) = {
    null is R
    run(tasksFactory)
}

public inline fun <T> call(f: () -> T): T = f()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

inline fun <reified R> foo() = bar<R, String>() {"OK"}

fun box(): String {
    return foo<String>()()
}
