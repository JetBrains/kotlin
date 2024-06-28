// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// DUMP_IR
// ISSUE: KT-64944

fun getInt(): Int? {
    return 1
}

fun test1() {
    val i1 = getInt()
    val i2 = getInt()

    var (int1: Int?, int2: Int?) = i1 to i2
    int1 = null
}

fun test2() {
    val i1 = getInt()
    val i2 = getInt()

    if (i1 == null || i2 == null) return

    var (int1: Int?, int2: Int?) = i1 to i2
    int1 = null
}

fun box(): String {
    test1()
    test2()
    return "OK"
}
