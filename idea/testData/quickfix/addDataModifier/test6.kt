// "Make 'Foo' data class" "true"
class Foo(private val bar: String, protected var baz: Int) {
    class A {
        fun test() {
            var (bar, baz) = Foo("A", 1)<caret>
        }
    }
}