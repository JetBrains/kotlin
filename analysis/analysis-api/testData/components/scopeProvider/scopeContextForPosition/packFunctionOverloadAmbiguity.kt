class Props {
    val color: String = ""
}

fun Text(text: String, ...Props.$props) {}

fun Text(value: Int, ...Props.$props) {}

fun Wrapper(...Text.$props) {
    <expr>text</expr>
    color.length
}
