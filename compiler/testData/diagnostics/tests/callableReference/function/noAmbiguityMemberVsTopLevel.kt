// !CHECK_TYPE

import kotlin.reflect.KFunction0

fun explicitlyExpectFunction0(f: () -> Unit) = f
fun explicitlyExpectFunction1(f: (A) -> Unit) = f

fun foo() {}

class A {
    fun foo() {}
    
    fun main() {
        val x = ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>foo<!>

        checkSubtype<KFunction0<Unit>>(x)

        explicitlyExpectFunction0(x)
        explicitlyExpectFunction1(<!TYPE_MISMATCH!>x<!>)

        explicitlyExpectFunction0(::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>foo<!>)
        explicitlyExpectFunction1(<!TYPE_MISMATCH!>::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>foo<!><!>)
    }
}
