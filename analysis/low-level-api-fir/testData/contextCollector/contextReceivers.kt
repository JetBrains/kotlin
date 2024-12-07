interface Foo {
    fun foo(): Int
}

interface Bar {
    fun bar(): Int
}

context(Foo, Bar)
fun test() {
    <expr>foo() + bar()</expr>
}