// FIR_IDENTICAL
import kotlinx.cinterop.*

fun foo(f: Function1<in Int, Int>) = f

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(::foo)
}
