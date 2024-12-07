// FIR_IDENTICAL
import kotlin.native.ref.*

class C(val x: Int) {
    fun bar(y: Int) = println(x + y)
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun foo(x: Int) {
    createCleaner(42, C(x)::bar)
}
