// IS_APPLICABLE: false

data class Bar(val foo: Int, val bar: Int)

open class Foo {
    <caret>var (foo, bar) = Bar(1, 1)
}