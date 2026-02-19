interface A {
    fun foo(): String
    val bar: String
}

class X(val a: String, var b: String) {
    fun foo(): String = "original class' method"
    val bar: String = "original class' property"
}

