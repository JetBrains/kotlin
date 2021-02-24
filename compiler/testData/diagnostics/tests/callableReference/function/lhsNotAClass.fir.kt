// !WITH_NEW_INFERENCE

class A<T, U : Any> {
    fun foo() = <!OTHER_ERROR!>T<!>::toString

    fun bar() = <!OTHER_ERROR!>U<!>::toString

    fun baz() {
        take(<!OTHER_ERROR!>T<!>::toString)

        take(<!OTHER_ERROR!>U<!>::toString)
    }
}

fun <T> foo() = <!OTHER_ERROR!>T<!>::toString

fun <U : Any> bar() = <!OTHER_ERROR!>U<!>::toString

fun take(arg: Any) {}
