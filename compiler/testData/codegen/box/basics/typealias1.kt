// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val x = Bar(42).x
    if (x != 42 ) return "FAIL: $x"
        return "OK"
}

class Foo(val x: Int)
typealias Bar = Foo
