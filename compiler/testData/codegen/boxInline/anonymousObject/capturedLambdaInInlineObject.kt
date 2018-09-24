// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

internal interface A<T> {
    fun run(): T;
}

internal inline fun bar(crossinline y: () -> String) = object : A<String> {
    override fun run() : String {
        return call(y)
    }
}

public inline fun <T> call(crossinline f: () -> T): T = object : A<T> {
    override fun run() : T {
        return f()
    }
}.run()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return bar { "OK" }.run()
}
