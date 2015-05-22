// PARAM_DESCRIPTOR: internal fun Bar.foo(): kotlin.Unit defined in root package
// PARAM_TYPES: Bar, Foo, kotlin.Any

open class Foo
class Bar : Foo()

fun Bar.foo() {
    <selection>toString()</selection>
}