// CHECK_TYPE

fun foo1(a: Int?, b: Array<Array<Int>>) {
    b[a!!][a<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>] = a
    checkSubtype<Int>(a)
}

fun foo2(a: Int?, b: Array<Array<Int>>) {
    b[0][a!!] = a
    checkSubtype<Int>(a)
}
