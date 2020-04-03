// "Make 'Foo' not open" "true"
class A {
    <caret>open companion object Foo {
        fun a(): Int = 1
    }
}