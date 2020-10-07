import kotlinx.cinterop.*

fun bar(x: Int) = x.signExtend<Float>()
