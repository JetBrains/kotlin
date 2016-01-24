
//KT-9517 Wrong resolve for invoke convention after smart cast
open class A {
    open val foo: () -> Number = null!!
}

class B: A() {
    override val foo: () -> Int
        get() = null!!
}

fun test(a: A) {
    if (a is B) {
        val <!UNUSED_VARIABLE!>foo<!>: Int = <!DEBUG_INFO_SMARTCAST!>a<!>.foo() // B::foo + invoke()
    }
}