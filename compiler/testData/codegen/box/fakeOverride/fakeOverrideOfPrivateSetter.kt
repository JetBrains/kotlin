
abstract class TextRendererActions1 {
    var fontSize: String = "OK"; private set
}

fun box() : String {
    return object : TextRendererActions1() {
        val glyph = this.fontSize
    }.glyph
}
