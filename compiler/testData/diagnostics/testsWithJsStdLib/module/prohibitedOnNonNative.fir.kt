package foo

@JsModule("A")
class A

@JsModule("B")
object B

@JsModule("foo")
fun foo() = 23

@JsModule("bar")
val bar = 42

@JsNonModule
val baz = 99
