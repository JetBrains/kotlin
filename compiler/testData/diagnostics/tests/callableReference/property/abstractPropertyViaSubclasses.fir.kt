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
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Number>(base.get(B()))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(base.get(C()))

    val a = A::x
    checkSubtype<KProperty1<A, String>>(a)
    checkSubtype<String>(a.get(A()))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Number>(a.<!INAPPLICABLE_CANDIDATE!>get<!>(B()))

    val b = B::x
    checkSubtype<KProperty1<B, Number>>(b)
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Int>(b.get(C()))
}
