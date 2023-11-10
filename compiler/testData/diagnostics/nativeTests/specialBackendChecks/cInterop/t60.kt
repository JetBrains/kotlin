// FIR_IDENTICAL
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*

class Vertex constructor(rawPtr: NativePtr) : CStructVar(rawPtr) {
    var x: Float = 0f
    var y: Float = 0f
    var r: Float = 0f
    var g: Float = 0f
    var b: Float = 0f
    @Suppress("DEPRECATION")
    companion object : CStructVar.Type(40, 8)
}
