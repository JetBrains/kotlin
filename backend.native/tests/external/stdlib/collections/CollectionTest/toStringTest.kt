import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    // we need toString() inside pattern because of KT-8666
    assertEquals("[1, a, null, ${Long.MAX_VALUE.toString()}]", listOf(1, "a", null, Long.MAX_VALUE).toString())
}
