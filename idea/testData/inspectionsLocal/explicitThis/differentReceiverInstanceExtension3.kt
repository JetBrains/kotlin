// WITH_RUNTIME
// PROBLEM: none

open class Foo

class SubFoo : Foo()

val Foo.bar: String get() = ""

fun SubFoo.func() = Foo().apply {
    <caret>this@func.bar
}
