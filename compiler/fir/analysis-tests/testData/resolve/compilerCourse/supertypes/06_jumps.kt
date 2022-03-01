// FILE: 1.kt
open class C : Some.B() {
    open class DerivedNested : Nested()
}

// FILE: 2.kt
class Some : Base() {
    open class B : A()
}

interface Foo
interface Bar

// FILE: 3.kt
open class Base {
    open class A {
        open class Nested
    }
}
