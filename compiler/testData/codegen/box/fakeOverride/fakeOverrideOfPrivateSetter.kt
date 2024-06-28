// JVM_ABI_K1_K2_DIFF: KT-63984

abstract class TextRendererActions1 {
    var fontSize: String = "OK"; private set
}

fun box() : String {
    return object : TextRendererActions1() {
        val glyph = this.fontSize
    }.glyph
}