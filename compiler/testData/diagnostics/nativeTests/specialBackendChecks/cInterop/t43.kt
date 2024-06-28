// FIR_IDENTICAL
import kotlinx.cinterop.*

@Suppress("DEPRECATION")
fun bar(x: Float) = x.signExtend<Int>()
