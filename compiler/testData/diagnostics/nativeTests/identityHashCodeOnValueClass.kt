import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

value class VC(val s: String)

@OptIn(ExperimentalNativeApi::class)
fun test(p: VC, p2: VC?, p3: Int, p4: Int?) {
    p.identityHashCode()
    p2.identityHashCode()
    p3.identityHashCode()
    p4.identityHashCode()

    with(p) { identityHashCode() }
}
