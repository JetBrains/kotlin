// RUN_PIPELINE_TILL: BACKEND
import kotlin.reflect.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun <T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

