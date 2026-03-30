open class Props {
    val a: Int = 0
    val b: String = ""
}

class Host : Props() {
    fun source(a: Int, b: String) {}

    fun fromType(...Props.$props) {}

    fun fromFunction(...source.$props) {}

    fun useBound(target: (Int, String) -> Unit) {
        target(...Props.$props(this))
        target(...Props.$props(this).exclude(a, b))
    }
}
