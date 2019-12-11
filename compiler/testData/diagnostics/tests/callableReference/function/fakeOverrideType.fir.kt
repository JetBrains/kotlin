// !CHECK_TYPE

import kotlin.reflect.KFunction2

open class A {
    fun foo(s: String): String = s
}

class B : A() {
}


fun test() {
    B::foo checkType { <!UNRESOLVED_REFERENCE!>_<!><KFunction2<B, String, String>>() }

    <!INAPPLICABLE_CANDIDATE!>(B::hashCode)("No.")<!>
}
