// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.ExperimentalValueClassesApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"

    @OptIn(ExperimentalNativeApi::class)
    fun inside() = identityHashCode()
}

@OptIn(ExperimentalNativeApi::class)
fun test(p: Wrapper, p2: Wrapper?) {
    p.identityHashCode()
    p2.identityHashCode()
    with(p) { identityHashCode() }
}
