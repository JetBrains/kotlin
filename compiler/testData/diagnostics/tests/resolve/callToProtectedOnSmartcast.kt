// ISSUE: KT-51827
abstract class A {
    abstract protected val a: Any?

    open class Nested(override val a: String) : A() {
        class B {
            fun f(other: A) {
                other.a
                if (other is Nested) {
                    <!DEBUG_INFO_SMARTCAST!>other<!>.a.length
                }
                if (other is C) {
                    <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>
                }
            }
        }
    }

    class C(override val a: String): Nested(a)
}
