class A
fun box(u: Int) {
    val x: A? = A()
    val y: A?
    if (u == 0) {
        y = x
    }
    else {
        y = null
    }

    y!!
}

// 0 IFNULL
// 1 IFNONNULL
// 1 throwNpe
// 0 ATHROW
