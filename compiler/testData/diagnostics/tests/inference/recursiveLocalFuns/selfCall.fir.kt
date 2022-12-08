fun foo() {
    fun bar1() = bar1()

    fun bar2() = 1 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> bar2()
    fun bar3() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>id<!>(bar3())
}

fun <T> id(x: T) = x
