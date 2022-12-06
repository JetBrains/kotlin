// FIR_IDE_IGNORE
fun foo() {
    fun bar1() = <!INFERENCE_ERROR!>bar1()<!>

    fun bar2() = 1 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!INFERENCE_ERROR!>bar2()<!>
    fun bar3() = id(<!INFERENCE_ERROR!>bar3()<!>)
}

fun <T> id(x: T) = x
