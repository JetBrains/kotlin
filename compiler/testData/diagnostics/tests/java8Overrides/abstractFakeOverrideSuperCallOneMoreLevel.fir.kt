// !LANGUAGE: -ForbidSuperDelegationToAbstractFakeOverride
interface Foo {
    fun check(): String = "OK"
}
abstract class Base {
    abstract fun check(): String
}

abstract class Derived : Base(), Foo
abstract class Derived2 : Derived() // ONE MORE LEVEL

class Problem : Derived2() {
    override fun check(): String {
        return super.<!ABSTRACT_SUPER_CALL_WARNING!>check<!>() // NO COMPILER ERROR, BUT FAILURE IN RUNTIME
    }
}
