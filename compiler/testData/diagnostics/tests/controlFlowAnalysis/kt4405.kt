//KT-4405 Control-flow analysis is not performed for some local declarations

package d

val closure = {
    val <!UNUSED_VARIABLE!>x4<!> = "" // error: should be UNUSED_VARIABLE

    fun g() {
        val <!UNUSED_VARIABLE!>x6<!> = "" // error: should be UNUSED_VARIABLE
    }

    fun h(): Int { // error: should be NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}

class A {
    init {
        fun foo(): Int {
        <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

        val <!UNUSED_VARIABLE!>closure<!> = {
            val <!UNUSED_VARIABLE!>x<!> = ""

            fun local(): Int {
            <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
        }

        val <!UNUSED_VARIABLE!>y<!> = ""
    }
}