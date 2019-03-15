class A
fun box() {
    val x: A? = A()
    val y: A?
    if (1 == 0) {
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
