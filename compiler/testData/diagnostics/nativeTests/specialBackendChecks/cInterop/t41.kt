// FIR_IDENTICAL
import kotlinx.cinterop.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar(x: Int) {

    fun foo() = x

    staticCFunction(::foo)
}
