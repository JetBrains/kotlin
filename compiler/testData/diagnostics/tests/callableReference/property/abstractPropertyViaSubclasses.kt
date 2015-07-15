// !CHECK_TYPE

import kotlin.reflect.KProperty1

interface Base {
    val x: Any
}

class A : Base {
    override val x: String = ""
}

open class B : Base {
    override val x: Number = 1.0
}

class C : B() {
    override val x: Int = 42
}

fun test() {
    val base = Base::x
    checkSubtype<KProperty1<Base, Any>>(base)
    checkSubtype<Any>(base.get(A()))
    checkSubtype<Number>(<!TYPE_MISMATCH!>base.get(B())<!>)
    checkSubtype<Int>(<!TYPE_MISMATCH!>base.get(C())<!>)

    val a = A::x
    checkSubtype<KProperty1<A, String>>(a)
    checkSubtype<String>(a.get(A()))
    checkSubtype<Number>(<!TYPE_MISMATCH!>a.get(<!TYPE_MISMATCH!>B()<!>)<!>)

    val b = B::x
    checkSubtype<KProperty1<B, Number>>(b)
    checkSubtype<Int>(<!TYPE_MISMATCH!>b.get(C())<!>)
}
