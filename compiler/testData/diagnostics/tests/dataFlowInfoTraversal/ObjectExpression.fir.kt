
fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    val a = object {
        fun baz() = bar(if (x == null) 0 else x)
        fun quux(): Int = if (x == null) x else x
    }
}
