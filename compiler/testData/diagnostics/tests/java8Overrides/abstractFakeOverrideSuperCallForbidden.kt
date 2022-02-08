// FIR_IDENTICAL
// !LANGUAGE: +ForbidSuperDelegationToAbstractFakeOverride +ForbidSuperDelegationToAbstractAnyMethod
interface Foo {
    fun check(): String = "OK"
}
abstract class Base {
    abstract fun check(): String
}
abstract class Derived : Base(), Foo

class Derived2 : Derived() {
    override fun check(): String {
        super<Derived>.<!ABSTRACT_SUPER_CALL!>check<!>()
        return super.<!ABSTRACT_SUPER_CALL!>check<!>()
    }
}

abstract class A {
    abstract override fun hashCode(): Int
}

interface I

class B : A(), I { // I is necessary here
    override fun hashCode() = super.<!ABSTRACT_SUPER_CALL!>hashCode<!>()
}
