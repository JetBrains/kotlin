class A {
    default object {
        fun invoke(i: Int) = i
    }
}

fun test() = <caret>A(1)