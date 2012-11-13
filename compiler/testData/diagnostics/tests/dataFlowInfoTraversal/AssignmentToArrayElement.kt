fun arrayAccessRHS(a: Int?, b: Array<Int>) {
    b[0] = a!!
    a : Int
}

fun arrayAccessLHS(a: Int?, b: Array<Int>) {
    b[a!!] = a
    a : Int
}

