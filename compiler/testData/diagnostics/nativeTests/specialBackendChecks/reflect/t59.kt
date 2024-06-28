// FIR_IDENTICAL
import kotlin.reflect.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <T : Comparable<T>> foo(block: () -> T) {
    typeOf<List<T>>()
}

