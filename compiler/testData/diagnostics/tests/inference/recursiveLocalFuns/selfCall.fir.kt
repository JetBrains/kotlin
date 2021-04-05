// !WITH_NEW_INFERENCE
fun foo() {
    fun bar1() = bar1()

    fun bar2() = 1 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> bar2()
    fun bar3() = id(<!ARGUMENT_TYPE_MISMATCH!>bar3()<!>)
}

fun <T> id(x: T) = x
