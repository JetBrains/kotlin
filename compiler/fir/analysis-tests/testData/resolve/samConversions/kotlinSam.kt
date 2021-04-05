fun interface MyRunnable {
    fun foo(x: Int): Boolean
}

fun interface WithProperty {
    val x: Int
}

fun interface TwoAbstract : MyRunnable {
    fun bar()
}

fun interface Super {
    fun foo(x: Int): Any
}

fun interface Derived : Super {
    override fun foo(x: Int): Boolean
}

fun foo1(m: MyRunnable) {}
fun foo2(m: WithProperty) {}
fun foo3(m: TwoAbstract) {}
fun foo4(m: Derived) {}

fun main() {
    val f = { t: Int -> t > 1}

    foo1 { x -> x > 1 }
    foo1(f)

    foo2 { x -> <!ARGUMENT_TYPE_MISMATCH!><!ARGUMENT_TYPE_MISMATCH!>x<!> <!UNRESOLVED_REFERENCE!>><!> 1<!> }
    foo2(<!ARGUMENT_TYPE_MISMATCH!>f<!>)

    foo3 { x -> <!ARGUMENT_TYPE_MISMATCH!><!ARGUMENT_TYPE_MISMATCH!>x<!> <!UNRESOLVED_REFERENCE!>><!> 1<!> }
    foo3(<!ARGUMENT_TYPE_MISMATCH!>f<!>)

    foo4 { x -> x > 1 }
    foo4(f)

}
