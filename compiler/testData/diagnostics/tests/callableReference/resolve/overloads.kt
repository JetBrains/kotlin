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
    B::foo // todo KT-9601 Chose maximally specific function in callable reference

    B::bar checkType { _<KFunction1<B, Unit>>() }

    B::<!OVERLOAD_RESOLUTION_AMBIGUITY!>bas<!>

    ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>fas<!>
}