// !JVM_TARGET: 1.8
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    fun test()<!> {

    }
}

interface B{
    fun test()
}

class <!JVM_DEFAULT_THROUGH_INHERITANCE!>C<!> : A, B {
    override fun test() {
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}

class <!JVM_DEFAULT_THROUGH_INHERITANCE!>D<!> : B, A {
    override fun test() {
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}