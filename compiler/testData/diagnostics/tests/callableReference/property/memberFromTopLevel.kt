// !CHECK_TYPE

import kotlin.reflect.*

class A {
    val foo: Int = 42
    var bar: String = ""
}

fun test() {
    val p = A::foo

    checkSubtype<KMemberProperty<A, Int>>(p)
    checkSubtype<KMutableMemberProperty<A, Int>>(<!TYPE_MISMATCH!>p<!>)
    checkSubtype<Int>(p.get(A()))
    p.get(<!NO_VALUE_FOR_PARAMETER!>)<!>
    p.<!UNRESOLVED_REFERENCE!>set<!>(A(), 239)

    val q = A::bar

    checkSubtype<KMemberProperty<A, String>>(q)
    checkSubtype<KMutableMemberProperty<A, String>>(q)
    checkSubtype<String>(q.get(A()))
    q.set(A(), "q")
}
