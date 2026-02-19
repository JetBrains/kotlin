// WITH_STDLIB
import kotlin.test.*

class Foo(val bar: Int)

var x = Foo(42)

fun box(): String {
    assertEquals(42, x.bar)
    return "OK"
}
