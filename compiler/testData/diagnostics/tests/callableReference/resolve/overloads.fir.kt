// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
import kotlin.reflect.KFunction1

open class A {
    open fun bar() {}

    fun bas() {}
}
class B: A() {
    override fun bar() {}

    fun bas(i: Int) {}
}

fun A.foo() {}
fun B.foo() {}

fun fas() {}
fun fas(i: Int = 1) {}

fun test() {
    <!UNRESOLVED_REFERENCE!>B::foo<!> // todo KT-9601 Chose maximally specific function in callable reference

    B::bar checkType { <!UNRESOLVED_REFERENCE!>_<!><KFunction1<B, Unit>>() }

    <!UNRESOLVED_REFERENCE!>B::bas<!>

    ::fas
}