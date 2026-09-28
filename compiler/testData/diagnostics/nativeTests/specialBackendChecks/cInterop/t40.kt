// RUN_PIPELINE_TILL: CODEGEN
import kotlinx.cinterop.*

class Z {
    fun foo(x: Int) = x
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun bar() {
    staticCFunction(Z()::foo)
}
