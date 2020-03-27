// !JVM_TARGET: 1.8
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!>
    fun test()<!> {

    }
}

interface B{
    fun test()
}
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>AB<!><!> : A, B
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>BA<!><!> : B, A

class C : A, B {
    override fun test() {
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}

class D : B, A {
    override fun test() {
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class <!JVM_DEFAULT_THROUGH_INHERITANCE!>E<!><!>: B, A {
    fun foo() {
        super<A>.<!USAGE_OF_JVM_DEFAULT_THROUGH_SUPER_CALL!>test<!>()
    }
}