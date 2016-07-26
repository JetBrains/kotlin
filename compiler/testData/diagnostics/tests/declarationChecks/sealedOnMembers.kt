interface A {
    <!WRONG_MODIFIER_TARGET!>sealed<!> fun foo()
    <!WRONG_MODIFIER_TARGET!>sealed<!> var bar: Unit
}

interface B {
    abstract fun foo()
    abstract var bar: Unit
}

interface C : A, B

abstract class D(<!WRONG_MODIFIER_TARGET!>sealed<!> var x: Int) {
    abstract var y: Unit
        <!WRONG_MODIFIER_TARGET!>sealed<!> set
}

abstract class E : D(42)
