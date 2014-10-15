// "Create function 'foo' from usage" "true"

class A<T>(val t: T) {
    val x: A<Int> by <caret>foo(t, "")
}
