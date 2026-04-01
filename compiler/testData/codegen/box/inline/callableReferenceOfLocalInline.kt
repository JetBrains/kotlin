// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// ^^^ Local inline functions are not yet supported.
// FILE: lib.kt
package foo

fun multiplyBy(x: Int): () -> ((Int) -> Int) {
    inline fun applyMultiplication(y: Int): Int = x * y

    return { ::applyMultiplication }
}

// FILE: main.kt
package foo
import kotlin.test.*

fun box(): String {
    assertEquals(6, multiplyBy(2)()(3))

    return "OK"
}