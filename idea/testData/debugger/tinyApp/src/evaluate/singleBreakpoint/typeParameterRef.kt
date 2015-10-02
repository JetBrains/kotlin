package typeParameterRef

fun main(args: Array<String>) {
    test<Int>()
}

fun foo<U>(): Int {
    return 1
}

fun test<T>() {
    //Breakpoint!
    val a = foo<T>()
}

// EXPRESSION: foo<T>()
// RESULT: 1: I