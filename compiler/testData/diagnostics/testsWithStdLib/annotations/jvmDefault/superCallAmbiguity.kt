// !JVM_TARGET: 1.8
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!>
    fun test()<!> {
    }
}

interface B{
    fun test() {
    }
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface AB<!>: A, B

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface BA<!>: B, A


interface C : A, B {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!>
    override fun test()<!> {
        super<B>.test()
        super<A>.test()
    }
}

interface D : B, A {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!>
    override fun test()<!> {
        super<B>.test()
        super<A>.test()
    }
}