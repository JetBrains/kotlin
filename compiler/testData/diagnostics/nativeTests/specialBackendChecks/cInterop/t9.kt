// FIR_IDENTICAL
import kotlinx.cinterop.*

fun foo(f: Function0<<!REDUNDANT_PROJECTION!>out<!> Int>) = f

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(::foo)
}
