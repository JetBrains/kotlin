// !CHECK_TYPE

import kotlin.reflect.KFunction1

fun foo() {}

class A {
    fun foo() {}
    
    fun main() {
        val x = ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>foo<!>

        checkSubtype<KFunction1<A, Unit>>(x)
    }
}
