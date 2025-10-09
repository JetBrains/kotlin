class Y: X, Z {
    override fun foo(): String = "overridden method"
    override val bar: String get() = "overridden property"
    override fun qux(): String = "overridden multiple versions"
}

val y = Y()
val t = object : Z {}

fun lib(): String = when {
    y.foo() != "overridden method" -> "fail 1"
    y.bar != "overridden property" -> "fail 2"
    y.qux() != "overridden multiple versions" -> "fail 3"
    t.qux() != "alternative default method" -> "fail 4"

    else -> "OK"
}

