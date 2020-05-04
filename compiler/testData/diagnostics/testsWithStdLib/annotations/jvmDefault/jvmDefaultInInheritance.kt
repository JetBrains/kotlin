// !JVM_TARGET: 1.8

interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    fun test()<!> {
    }
}

interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>B<!> : A {

}


open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo<!> : B
open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo2<!> : B, A

open class FooNoError : B {
    override fun test() {
    }
}
open class Foo2NoError : B, A {
    override fun test() {
    }
}

class Bar : Foo()
class Bar2 : Foo(), A
class Bar3 : Foo(), B

open class BarWithJvmDefault : B {
    <!JVM_DEFAULT_NOT_IN_INTERFACE!>@JvmDefault<!>
    override fun test() {
    }
}

class BarWithJvmDefaultSuper: BarWithJvmDefault()
