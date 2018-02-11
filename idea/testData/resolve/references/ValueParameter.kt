open class B
class A
val prop = object : B() {
    private fun foo(x: A): A {
        return <caret>x
    }
}

// REF: (in object in prop).x
