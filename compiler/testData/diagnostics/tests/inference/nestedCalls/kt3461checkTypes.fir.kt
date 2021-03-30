//KT-3461 Nullable argument allowed where shouldn't be
package a

class F {
    fun p(): String? = null
}

fun foo(s: String) {}

fun r(): Int? = null

fun test() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>F().p()<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>r()<!>)
}