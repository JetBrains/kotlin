// !CHECK_TYPE

import kotlin.reflect.KFunction0

class A {
    class Nested
}

fun main() {
    val x = A::Nested

    checkSubtype<KFunction0<A.Nested>>(x)
}
