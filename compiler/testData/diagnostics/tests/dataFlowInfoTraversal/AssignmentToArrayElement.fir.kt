// CHECK_TYPE

fun arrayAccessRHS(a: Int?, b: Array<Int>) {
    b[0] = a!!
    checkSubtype<Int>(a)
}

fun arrayAccessLHS(a: Int?, b: Array<Int>) {
    b[a!!] = a
    checkSubtype<Int>(a)
}

