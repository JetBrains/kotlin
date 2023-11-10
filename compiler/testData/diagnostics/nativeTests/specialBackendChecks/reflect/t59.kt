// FIR_IDENTICAL
import kotlin.reflect.*

fun <T : Comparable<T>> foo() {
    typeOf<List<T>>()
}

