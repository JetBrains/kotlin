sealed class C<out T, out U>
class A<out T>(val x: T) : C<T, Nothing>()
class B<out U>(val x: U) : C<Nothing, U>()

fun bar(x: String): C<Int, String> = B(x)
fun baz(x: Any) = "fail: $x"
fun baz(x: String) = x

typealias Z<U> = B<U>

fun box(): String =
    when (val x = bar("O")) {
        is A -> "fail??"
        is B -> baz(x.x)
    } + when (val y = bar("K")) {
        is A -> "fail??"
        is Z -> baz(y.x)
        else -> "..."
    }
