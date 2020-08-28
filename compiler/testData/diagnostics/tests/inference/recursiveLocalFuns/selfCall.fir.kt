// !WITH_NEW_INFERENCE
fun foo() {
    fun bar1() = bar1()

    fun bar2() = 1 <!AMBIGUITY!>+<!> bar2()
    fun bar3() = <!INAPPLICABLE_CANDIDATE!>id<!>(bar3())
}

fun <T> id(x: T) = x
