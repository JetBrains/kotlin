interface Foo {
    fun foo(): Int
}

context(Foo)
class Test {
    fun test() {
        <expr>foo()</expr>
    }
}