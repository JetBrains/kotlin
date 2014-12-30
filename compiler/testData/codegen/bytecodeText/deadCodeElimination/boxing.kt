fun foo(x: Any?) {}

fun box() {
    val x: Int? = 1
    x!!

    val z: Int? = if (1 == 1) x else null
    z!!

    foo(1 as java.lang.Integer)
}

// 0 IFNULL
// 1 IFNONNULL
// 1 throwNpe
// 0 ATHROW
