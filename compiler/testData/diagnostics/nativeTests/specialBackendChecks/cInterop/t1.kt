// FIR_IDENTICAL
import kotlinx.cinterop.*

fun foo(x: Int, vararg s: String): Int {
    var sum = x
    s.forEach { sum += it.length }
    return sum
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(::foo)
}
