// "Create object 'Foo'" "true"
// DISABLE-ERRORS

open class B

class A {
    val x: B by <caret>Foo
}
