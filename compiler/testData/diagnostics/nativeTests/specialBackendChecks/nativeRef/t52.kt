// RUN_PIPELINE_TILL: BACKEND
import kotlin.native.ref.*

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
fun foo(x: Int) {
    createCleaner(42) { println(x) }
}
