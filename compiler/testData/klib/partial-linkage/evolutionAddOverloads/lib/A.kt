class X {
    fun foo(s: String): String = "initial overload $s"
    fun bar(s: String?): String = "initial nullable overload $s"
    fun qux(s: Any): String = "initial any overload $s"
}

fun foo(s: String): String = "initial overload $s"
fun bar(s: String?): String = "initial nullable overload $s"
fun qux(s: Any): String = "initial any overload $s"

