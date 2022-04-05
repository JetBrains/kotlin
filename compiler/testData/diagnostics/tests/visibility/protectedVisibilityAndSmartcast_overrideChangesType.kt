// FIR_DUMP

interface Base {
    fun baseFun()
}

interface Derived : Base {
    fun derivedFun()
}

abstract class A {
    abstract protected val a: Base

    fun fest_1(other: A) {
        other.a.baseFun() // OK
        if (other is B) {
            <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
            <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
        }
        if (other is C) {
            <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
            <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
        }
        if (other is D) {
            <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
            <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
        }
    }

    open class B(override val a: Derived) : A() {
        class Nested {
            fun fest_3(other: A) {
                other.a.baseFun() // OK
                if (other is B) {
                    <!DEBUG_INFO_SMARTCAST!>other<!>.a.baseFun()
                    <!DEBUG_INFO_SMARTCAST!>other<!>.a.derivedFun()
                }
                if (other is C) {
                    <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
                    <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
                }
                if (other is D) {
                    <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
                    <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
                }
            }
        }
    }

    class C(override val a: Derived) : B(a) {
        fun fest_4(other: A) {
            other.a.baseFun() // OK
            if (other is B) {
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
            }
            if (other is C) {
                <!DEBUG_INFO_SMARTCAST!>other<!>.a.baseFun()
                <!DEBUG_INFO_SMARTCAST!>other<!>.a.derivedFun()
            }
            if (other is D) {
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
            }
        }
    }

    class D(override val a: Derived) : A() {
        fun fest_5(other: A) {
            other.a.baseFun() // OK
            if (other is B) {
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
            }
            if (other is C) {
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.baseFun()
                <!DEBUG_INFO_SMARTCAST!>other<!>.<!INVISIBLE_MEMBER!>a<!>.derivedFun()
            }
            if (other is D) {
                <!DEBUG_INFO_SMARTCAST!>other<!>.a.baseFun()
                <!DEBUG_INFO_SMARTCAST!>other<!>.a.derivedFun()
            }
        }
    }
}
