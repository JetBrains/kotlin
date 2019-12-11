// !CHECK_TYPE

import kotlin.reflect.KFunction0

class A {
    class Nested
}

fun main() {
    val x = A::Nested

    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><KFunction0<A.Nested>>(x)
}
