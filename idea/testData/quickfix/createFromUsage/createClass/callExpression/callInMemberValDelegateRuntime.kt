// "Create class 'Foo'" "true"

open class B

class A<T>(val t: T) {
    val x: B by <caret>Foo(t, "")
}
