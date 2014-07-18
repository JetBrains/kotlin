class A {
    class object {
        fun invoke(i: Int) = i
    }
}

fun test() = A<caret>(1)