// !JVM_TARGET: 1.8
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@<!DEPRECATION!>JvmDefault<!><!>
    fun test() {

    }
}

interface B{
    fun test()
}
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>AB<!><!> : A, B
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>BA<!><!> : B, A

class C : A, B {
    override fun test() {
        super<A>.test()
    }
}

class D : B, A {
    override fun test() {
        super<A>.test()
    }
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class <!JVM_DEFAULT_THROUGH_INHERITANCE!>E<!><!>: B, A {
    fun foo() {
        super<A>.test()
    }
}
