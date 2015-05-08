// "Create function 'foo'" "true"

class A<T>(val t: T) {
    var x: A<Int> by <caret>foo(t, "")
}
