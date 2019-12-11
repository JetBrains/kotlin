// !DIAGNOSTICS: -UNUSED_PARAMETER

object Foo1
object Foo2

fun foo(vararg ss: String) = Foo1
fun foo(x: Any) = Foo2

val test1: Foo1 = foo("")