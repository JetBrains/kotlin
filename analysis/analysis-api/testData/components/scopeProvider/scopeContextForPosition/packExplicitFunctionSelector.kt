fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textString: (text: String, color: String) -> Unit = ::Text

fun Wrapper(...Text.$props(textString)) {
    <expr>text</expr>
    color.length
}
