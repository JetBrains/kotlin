class TextPack(
    val color: String,
    val fontSize: Int,
)

fun renderLabel(text: String, ...TextPack.$props): String {
    return "$text|$color|$fontSize"
}
