// "Create class 'Foo'" "true"
// DISABLE-ERRORS

open class B

class A<T>(val t: T) {
    var x: B by <caret>Foo(t, "")
}
