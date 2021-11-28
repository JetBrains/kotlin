class A {
    operator fun component1() = 42
    operator fun component2() = 42
}

fun foo(a: A, c: Int) {
    val (<!NAME_SHADOWING!>a<!>, b) = a
    val arr = Array(2) { A() }
    for ((<!NAME_SHADOWING!>c<!>, d) in arr)  {

    }

    val e = a.toString() + b + c
}
