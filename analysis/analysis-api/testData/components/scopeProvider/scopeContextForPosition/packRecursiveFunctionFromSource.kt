open class Props {
    val a: Int = 0
    val b: String = ""
}

fun leaf(...Props.$props) {}

fun mid(...leaf.$props) {}

fun target(...mid.$props) {
    <expr>a</expr>
    b.length
}
