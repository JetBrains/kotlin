class Y: X() {
    override fun foo() = "derived body"
    override val bar get() = "derived body"
}

fun lib(): String {
    val y = Y()
    return when {
        y.foo() != "derived body" -> "fail 1"
        y.bar != "derived body" -> "fail 2"
        y.qux() != "derived bodyderived body" -> "fail 3"

        else -> "OK"
    }
}

