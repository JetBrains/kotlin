open class A {
    open protected val number: Number = 10
    open protected var count: Number = 20
    open val score: Number = 30
}

open class B : A() {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>override<!> val number = 20
        public get(): Number

    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>override<!> var <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>count<!> = 20
        public get(): Number

    protected override val score = 40
        public get(): Number
}

class C : B() {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE, INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>override<!> val score = 50
        public get(): Number
}
