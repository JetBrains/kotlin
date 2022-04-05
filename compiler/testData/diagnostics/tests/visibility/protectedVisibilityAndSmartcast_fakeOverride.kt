// FIR_IDENTICAL
interface Base {
    fun baseFun()
}

interface Derived : Base {
    fun derivedFun()
}

abstract class A<T : Base> {
    protected val a: T = null!!

    fun fest_1(other: A<*>) {
        other.a.baseFun() // OK
        if (other is B) {
            other.a.baseFun()
            other.a.derivedFun()
        }
        if (other is C) {
            other.a.baseFun()
            other.a.derivedFun()
        }
        if (other is D) {
            other.a.baseFun()
            other.a.derivedFun()
        }
    }

    open class B : A<Derived>() {
        class Nested {
            fun fest_3(other: A<*>) {
                other.a.baseFun() // OK
                if (other is B) {
                    other.a.baseFun()
                    other.a.derivedFun()
                }
                if (other is C) {
                    other.a.baseFun()
                    other.a.derivedFun()
                }
                if (other is D) {
                    other.a.baseFun()
                    other.a.derivedFun()
                }
            }
        }
    }

    class C : B() {
        fun fest_4(other: A<*>) {
            other.a.baseFun() // OK
            if (other is B) {
                other.a.baseFun()
                other.a.derivedFun()
            }
            if (other is C) {
                other.a.baseFun()
                other.a.derivedFun()
            }
            if (other is D) {
                other.a.baseFun()
                other.a.derivedFun()
            }
        }
    }

    class D : A<Derived>() {
        fun fest_5(other: A<*>) {
            other.a.baseFun() // OK
            if (other is B) {
                other.a.baseFun()
                other.a.derivedFun()
            }
            if (other is C) {
                other.a.baseFun()
                other.a.derivedFun()
            }
            if (other is D) {
                other.a.baseFun()
                other.a.derivedFun()
            }
        }
    }
}
