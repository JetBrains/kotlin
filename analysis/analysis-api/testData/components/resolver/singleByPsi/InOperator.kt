class Foo {
    operator fun contains(v: Int) = true
}
fun foo() = 0 <caret>in Foo()
