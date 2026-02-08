// IGNORE_BACKEND: JVM_IR
// ^^^ Local inline functions are not yet supported.
package foo
import kotlin.test.*

fun multiplyBy(x: Int): () -> ((Int) -> Int) {
    inline fun applyMultiplication(y: Int): Int = x * y

    return { ::applyMultiplication }
}

fun box(): String {
    assertEquals(6, multiplyBy(2)()(3))

    return "OK"
}