// RUN_PIPELINE_TILL: BACKEND
import kotlinx.cinterop.*

@Suppress("DEPRECATION")
fun bar(x: Float) = x.signExtend<Int>()
