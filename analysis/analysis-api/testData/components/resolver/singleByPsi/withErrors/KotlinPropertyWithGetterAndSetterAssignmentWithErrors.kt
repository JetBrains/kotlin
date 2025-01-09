class A {
    var something: Int
        set(value) {}
        get() = 10
}

fun A.foo(a: A) {
    (<caret_1>something)++
    (<caret_2>something) = 1
    (a.<caret_3>something) = 1
}

operator fun Int?.plus(i: Int): Int = 0
