// !JVM_TARGET: 1.8
interface A {
    @JvmDefault
    fun test() {

    }
}

interface B{
    fun test()
}
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface AB<!> : A, B
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface BA<!> : B, A

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

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class E<!>: B, A {
    fun foo() {
        super<A>.test()
    }
}