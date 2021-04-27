open class A {
    open fun test(): Number = 10
}

open class B : <!SUPERTYPE_NOT_INITIALIZED!>A<!> {
    override fun test(): Double = 20.0
    fun test(x: Int) = x
}

class C() : A() {
    override fun test(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String<!> = "Test"
}

open class D() : B() {
    override fun test(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Char<!> = '\n'
}

class E<T : <!FINAL_UPPER_BOUND!>Double<!>>(val value: T) : B() {
    override fun test(): T = value
}

open class F<T : Number>(val value: T) {
    open fun rest(): T = value
}

class G<E : <!FINAL_UPPER_BOUND!>Double<!>>(val balue: E) : F<E>(balue) {
    override fun rest(): E = balue
}

class H<E : <!FINAL_UPPER_BOUND!>String<!>>(val balue: E) : F<E>(<!ARGUMENT_TYPE_MISMATCH!>balue<!>) {
    override fun rest(): E = balue // no report because of INAPPLICABLE_CANDIDATE
}

class M<E : <!FINAL_UPPER_BOUND!>String<!>>(val balue: E) : F<Double>(3.14) {
    override fun rest(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>E<!> = balue
}

open class X
open class Y() : X()
open class Z() : Y()
open class W() : Z()

open class V {
    open fun hello(): X = X()
}

open class L() : V()

open class Q() : L()

open class S() : Q() {
    override fun hello(): W = W()
}

open class J() : S() {
    override fun hello(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Z<!> = Z()
}

open class Base<T : X, Z : T> {
    open fun kek(): Z = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>Z()<!>
}

open class GoodDerrived : Base<Y, W>() {
    override fun kek(): W = W()
}

open class BadDerrived : Base<Y, W>() {
    override fun kek(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String<!> = "test"
}
