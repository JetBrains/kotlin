// FIR_IDENTICAL
import kotlinx.cinterop.*

fun foo(f: Function0<*>) = f

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(::foo)
}
