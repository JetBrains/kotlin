// IGNORE_BACKEND: JVM_IR
// ^^^ Local inline functions are not yet supported.
package foo
import kotlin.test.*

fun multiplyBy(a: Int): (Int) -> Int {
    inline fun multiply(b: Int): Int = a * b

    return ::multiply
}

fun box(): String {
    assertEquals(6, multiplyBy(2)(3))

    return "OK"
}