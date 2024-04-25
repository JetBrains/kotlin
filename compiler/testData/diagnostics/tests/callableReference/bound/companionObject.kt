// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_EXPRESSION

package test

import checkSubtype

class C {
    companion object {
        fun foo(): String = "companion"
        fun bar() {}
    }

    fun foo(): Int = 0
}

fun test() {
    val r1 = C::foo
    checkSubtype<(C) -> Int>(r1)

    val r2 = test.C::foo
    checkSubtype<(C) -> Int>(r2)

    val r3 = C.Companion::foo
    checkSubtype<() -> String>(r3)

    val r4 = test.C.Companion::foo
    checkSubtype<() -> String>(r4)

    val r5 = <!PARENTHESIZED_COMPANION_LHS_DEPRECATION!>(C)<!>::foo
    checkSubtype<() -> String>(r5)

    val r6 = <!PARENTHESIZED_COMPANION_LHS_DEPRECATION!>(test.C)<!>::foo
    checkSubtype<() -> String>(r6)

    val c = C.Companion
    val r7 = c::foo
    checkSubtype<() -> String>(r7)

    <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>C::bar<!>
}
