// RUN_PIPELINE_TILL: CODEGEN
import kotlinx.cinterop.*

@Suppress("DEPRECATION")
fun bar(x: Float) = x.signExtend<Int>()
