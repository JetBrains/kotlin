// WITH_RUNTIME

open class Foo

class SubFoo : Foo()

val SubFoo.bar: String get() = ""

fun SubFoo.func() = Foo().apply {
    <caret>this@func.bar
}
