class Z: Y() {
    override fun foo() = "primordial overridden function"
    override val bar = "primordial overridden property"
}

fun lib(): String {
    val y = Y()
    val z = Z()
    return when {
        y.foo() != "base function" -> "fail 1"
        y.bar != "base property" -> "fail 2"

        z.foo() != "primordial overridden function" -> "fail 5"
        z.bar != "primordial overridden property" -> "fail 6"

        else -> "OK"
    }
}

