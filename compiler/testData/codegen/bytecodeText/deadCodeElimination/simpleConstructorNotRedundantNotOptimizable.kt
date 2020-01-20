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
// 0 IFNONNULL
// 0 throwNpe
// 1 checkNotNull \(Ljava/lang/Object;\)V
// 0 ATHROW
