fun bar(x: String): Int = 1
fun bar(x: String): Double = 1

fun baz(x: String): Int = 1
fun <T, R> foobaz(x: T): R = TODO()

fun foo() {
    val x: (String) -> Int = ::bar
    val y = <!UNRESOLVED_REFERENCE!>::bar<!>
    val z = ::baz
    val w: (String) -> Int = ::foobaz

    ::baz
}
