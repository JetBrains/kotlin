// FILE: 1.kt
package test

inline fun f(crossinline g: () -> Unit) = Runnable(object : () -> Unit {
    override fun invoke() = g()
})

// FILE: 2.kt
import test.*

fun box(): String {
    var result = "FAIL"
    f { result = "OK" }.run()
    return result
}
