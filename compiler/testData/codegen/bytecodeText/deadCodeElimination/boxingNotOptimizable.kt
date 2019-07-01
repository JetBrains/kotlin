class A

fun foo(x: Any?) {}

fun box(u: Int) {
    val x: Int? = 1
    x!!

    val z: Int? = if (u == 1) x else null
    z!!

    foo(1 as java.lang.Integer)

    val y: Any? = if (u == 1) x else A()
    y!!
}

// 0 IFNULL
// 1 IFNONNULL
// 1 throwNpe
// 0 ATHROW
