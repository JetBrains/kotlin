fun Text(text: String, color: String) {}

fun Text(value: Int, color: String) {}

val textString: (text: String, color: String) -> Unit = ::Text

fun Wrapper(...textString.$props) {
    <expr>text</expr>
    color.length
}
