// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract
// ERROR: Variable 'foo' must be initialized

class A<T> {
    var x: A<Int> by <caret>foo
}
