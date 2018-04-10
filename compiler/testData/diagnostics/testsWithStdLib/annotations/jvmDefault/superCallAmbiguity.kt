// !JVM_TARGET: 1.8
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    fun test()<!> {
    }
}

interface B{
    fun test() {
    }
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>AB<!><!>: A, B

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>BA<!><!>: B, A


interface C : A, B {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    override fun test()<!> {
        super<B>.test()
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}

interface D : B, A {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    override fun test()<!> {
        super<B>.test()
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}