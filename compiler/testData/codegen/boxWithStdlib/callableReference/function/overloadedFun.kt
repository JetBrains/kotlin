import kotlin.test.assertEquals

fun foo(): String = "foo1"
fun foo(i: Int): String = "foo2"

val f1: () -> String = ::foo
val f2: (Int) -> String = ::foo

fun foo1() {}
fun foo2(i: Int) {}

fun bar(f: () -> Unit): String = "bar1"
fun bar(f: (Int) -> Unit): String = "bar2"

fun box(): String {
    assertEquals("foo1", f1())
    assertEquals("foo2", f2(0))
    assertEquals("bar1", bar(::foo1))
    assertEquals("bar2", bar(::foo2))

    return "OK"
}