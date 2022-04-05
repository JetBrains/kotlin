// FIR_IDENTICAL
interface Base {
    fun baseFun()
}

abstract class A {
    protected val a: Base = null!!

    fun fest_1(other: A) {
        other.a.baseFun() // OK
        if (other is B) {
            other.a.baseFun()
        }
        if (other is C) {
            other.a.baseFun()
        }
        if (other is D) {
            other.a.baseFun()
        }
    }

    open class B : A() {
        class Nested {
            fun fest_3(other: A) {
                other.a.baseFun() // OK
                if (other is B) {
                    other.a.baseFun()
                }
                if (other is C) {
                    other.a.baseFun()
                }
                if (other is D) {
                    other.a.baseFun()
                }
            }
        }
    }

    class C : B() {
        fun fest_4(other: A) {
            other.a.baseFun() // OK
            if (other is B) {
                other.a.baseFun()
            }
            if (other is C) {
                other.a.baseFun()
            }
            if (other is D) {
                other.a.baseFun()
            }
        }
    }

    class D : A() {
        fun fest_5(other: A) {
            other.a.baseFun() // OK
            if (other is B) {
                other.a.baseFun()
            }
            if (other is C) {
                other.a.baseFun()
            }
            if (other is D) {
                other.a.baseFun()
            }
        }
    }
}
