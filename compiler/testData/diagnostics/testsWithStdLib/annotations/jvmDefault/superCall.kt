// !JVM_TARGET: 1.8
// FILE: 1.kt
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    fun test()<!> {
    }
}

// FILE: 2.kt
interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>B<!> : A {

}

interface C : B {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    override fun test()<!> {
        super.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}

open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo<!> : B {
    override fun test() {
        super.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}
open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo2<!> : B

open class Bar : Foo2() {
    override fun test() {
        super.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}

class Bar2 : Bar() {
    override fun test() {
        super.test()
    }
}