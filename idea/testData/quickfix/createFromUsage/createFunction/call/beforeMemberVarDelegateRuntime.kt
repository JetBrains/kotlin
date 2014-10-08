// "Create function 'foo' from usage" "true"

class A<T>(val t: T) {
    var x: A<Int> by <caret>foo(t, "")
}
