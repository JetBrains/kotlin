interface A {
    <!WRONG_MODIFIER_TARGET!>sealed<!> fun foo()
    <!WRONG_MODIFIER_TARGET!>sealed<!> var bar: Unit
}

interface B {
    abstract fun foo()
    abstract var bar: Unit
}

interface C : A, B

abstract class D(<!SEALEDARG_PARAMETER_WRONG_CLASS!>sealed<!> var x: Int) {
    abstract var y: Unit
        <!WRONG_MODIFIER_TARGET!>sealed<!> set
}

abstract class E : D(<!SEALED_ARGUMENT_NO_CONSTRUCTOR!>42<!>)
