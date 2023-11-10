// FIR_IDENTICAL
import kotlinx.cinterop.*

@Suppress("DEPRECATION")
fun bar(x: Int) = x.narrow<Long>()
