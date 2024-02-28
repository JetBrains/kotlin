// ISSUE: KT-51827
abstract class A {
    abstract protected val a: Any?

    open class Nested(override val a: String) : A() {
        class B {
            fun f(other: A) {
                other.a
                if (other is Nested) {
                    other.a.length
                }
                if (other is C) {
                    other.a
                }
            }
        }
    }

    class C(override val a: String): Nested(a)
}
