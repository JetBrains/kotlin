// !CHECK_TYPE
// !LANGUAGE: +CallableReferencesToClassMembersWithEmptyLHS

import kotlin.reflect.KFunction0

fun expectFunction0Unit(f: () -> Unit) = f
fun expectFunction0String(f: () -> String) = f
fun expectFunction1Unit(f: (A) -> Unit) = f
fun expectFunction1String(f: (A) -> String) = f

fun foo(): String = ""

class A {
    fun foo() {}
    
    fun main() {
        val x = ::foo

        checkSubtype<KFunction0<Unit>>(x)

        expectFunction0Unit(x)
        <!INAPPLICABLE_CANDIDATE!>expectFunction0String<!>(x)
        <!INAPPLICABLE_CANDIDATE!>expectFunction1Unit<!>(x)
        <!INAPPLICABLE_CANDIDATE!>expectFunction1String<!>(x)

        expectFunction0Unit(::foo)
        expectFunction0String(::foo)
        <!INAPPLICABLE_CANDIDATE!>expectFunction1Unit<!>(<!UNRESOLVED_REFERENCE!>::foo<!>)
        <!INAPPLICABLE_CANDIDATE!>expectFunction1String<!>(<!UNRESOLVED_REFERENCE!>::foo<!>)
    }
}
