//KT-3461 Nullable argument allowed where shouldn't be
package a

class F {
    fun p(): String? = null
}

fun foo(s: String) {}

fun r(): Int? = null

fun test() {
    foo(<!TYPE_MISMATCH!>F().p()<!>)
    foo(<!TYPE_MISMATCH!>r()<!>)
}
