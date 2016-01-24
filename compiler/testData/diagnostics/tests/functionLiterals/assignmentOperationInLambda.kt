// !CHECK_TYPE

fun test(bal: Array<Int>) {
    var bar = 4

    val a = { bar += 4 }
    checkSubtype<() -> Unit>(a)

    val b = { bar = 4 }
    checkSubtype<() -> Unit>(b)

    val c = { bal[2] = 3 }
    checkSubtype<() -> Unit>(c)

    val d = run { bar += 4 }
    checkSubtype<Unit>(d)
}
