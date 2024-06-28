// FIR_IDENTICAL
import kotlinx.cinterop.*

fun foo(f: Function1<<!REDUNDANT_PROJECTION!>in<!> Int, Int>) = f

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(::foo)
}
