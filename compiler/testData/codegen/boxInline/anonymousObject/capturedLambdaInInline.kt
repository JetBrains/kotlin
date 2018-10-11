// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

inline fun bar(crossinline y: () -> String) = {
    call(y)
}

public inline fun <T> call(f: () -> T): T = f()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return bar {"OK"} ()
}
