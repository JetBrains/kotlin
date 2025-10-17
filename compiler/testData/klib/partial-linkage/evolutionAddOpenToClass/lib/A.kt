class X {
    fun foo(): String = "in final class"
    val bar: String = "in final class"
}

fun qux(): X = X()

