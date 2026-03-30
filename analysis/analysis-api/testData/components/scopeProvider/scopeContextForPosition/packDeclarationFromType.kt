open class Props {
    val a: Int = 0
    val b: String = ""
}

fun target(...Props.$props) {
    <expr>a</expr>
    b.length
}
