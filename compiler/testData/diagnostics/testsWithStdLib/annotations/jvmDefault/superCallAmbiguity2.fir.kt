// !JVM_TARGET: 1.8
interface A {
    @JvmDefault
    fun test()
}

interface B{
    fun test() {
    }
}

interface AB: A, B

interface BA: B, A

class C : A, B {
    override fun test() {
        super<B>.test()
    }
}

class D : B, A {
    override fun test() {
        super<B>.test()
    }
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class E<!>: B, A {
    fun foo() {
        super<B>.test()
    }
}