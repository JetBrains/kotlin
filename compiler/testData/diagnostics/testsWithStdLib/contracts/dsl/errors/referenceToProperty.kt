// !LANGUAGE: +ReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

class Foo(val x: Int?) {
    fun isXNull(): Boolean {
        contract {
            returns(false) implies (<!ERROR_IN_CONTRACT_DESCRIPTION(only references to parameters are allowed in contract description)!>x<!> != null)
        }
        return x != null
    }
}