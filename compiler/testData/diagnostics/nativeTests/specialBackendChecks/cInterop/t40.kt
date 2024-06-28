// FIR_IDENTICAL
import kotlinx.cinterop.*

class Z {
    fun foo(x: Int) = x
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(Z()::foo)
}
