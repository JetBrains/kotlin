class A
fun box() {
    val x: A? = A()
    val z: A? = A()
    val z1: A? = if (1 == 1) z else x
    
    x!!
    z!!
    z1!!
}

// 0 IFNULL
// 0 IFNONNULL
// 0 throwNpe
// 0 ATHROW
