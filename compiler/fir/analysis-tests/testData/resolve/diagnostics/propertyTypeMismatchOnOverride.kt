open class A {
    open var test: Number = 10
}

open class B : A {
    override var test: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Double<!> = 20.0
}

class C() : A() {
    override var test: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>String<!> = "Test"
}

open class D() : B() {
    override var test: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Char<!> = '\n'
}

class E<T : Double>(val value: T) : B() {
    override var test: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>T<!> = value
}

open class F<T : Number>(val value: T) {
    open var rest: T = value
}

class G<E : Double>(val balue: E) : F<E>(balue) {
    override var rest: E = balue
}

class H<E : String>(val balue: E) : <!INAPPLICABLE_CANDIDATE{LT}!><!INAPPLICABLE_CANDIDATE{PSI}!>F<E><!>(balue)<!> {
    override var rest: E = balue // no report because of INAPPLICABLE_CANDIDATE
}

class M<E : String>(val balue: E) : F<Double>(3.14) {
    override var rest: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>E<!> = balue
}
