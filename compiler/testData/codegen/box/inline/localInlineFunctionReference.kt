// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// ^^^ Local inline functions are not yet supported.
// FILE: lib.kt
package foo
import kotlin.test.*

fun multiplyBy(a: Int): (Int) -> Int {
    inline fun multiply(b: Int): Int = a * b

    return ::multiply
}

// FILE: main.kt
package foo
import kotlin.test.*

fun box(): String {
    assertEquals(6, multiplyBy(2)(3))

    return "OK"
}