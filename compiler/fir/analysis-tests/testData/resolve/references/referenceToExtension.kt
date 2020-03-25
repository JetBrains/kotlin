class GenericTest {
    class A<T>

    class B<T> {
        val memberVal: A<T> = A()
        fun memberFun(): A<T> = A()
    }

    val <T> B<T>.extensionVal: A<T>
        get() = A()

    fun <T> B<T>.extensionFun(): A<T> = A()

    fun test_1() {
        val memberValRef = B<*>::memberVal
        val memberFunRef = B<*>::memberFun
    }

    fun test_2() {
        val extensionValRef = <!UNRESOLVED_REFERENCE!>B<*>::extensionVal<!>
        val extensionFunRef = <!UNRESOLVED_REFERENCE!>B<*>::extensionFun<!>
    }
}

class NoGenericTest {
    class A

    class B {
        val memberVal: A = A()
        fun memberFun(): A = A()
    }

    val B.extensionVal: A
        get() = A()

    fun B.extensionFun(): A = A()

    fun test_1() {
        val extensionValRef = <!UNRESOLVED_REFERENCE!>B::extensionVal<!>
        val extensionFunRef = <!UNRESOLVED_REFERENCE!>B::extensionFun<!>
    }

    fun test_2() {
        val memberValRef = B::memberVal
        val memberFunRef = B::memberFun
    }
}