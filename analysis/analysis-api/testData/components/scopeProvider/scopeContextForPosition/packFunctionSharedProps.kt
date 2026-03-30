fun Text(text: String, color: String, modifier: Int = 0) {}

fun Text(text: Int, color: String, modifier: Int = 0) {}

fun Wrapper(...Text.$sharedProps) {
    <expr>color</expr>
    modifier + 1
}
