open class Props {
    val a: Int = 0
    val b: String = ""
}

fun source(...Props.$props) {}

fun target(...source.$props) {
    <expr>a</expr>
    b.length
}
