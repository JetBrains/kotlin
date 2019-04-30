interface B
interface C {
    val b: B
}

fun A(b: B?, flag: Boolean = true) = A(b!!, flag)

fun A(c: C, flag: Boolean = true) = A(c.b, flag)

class A(val b: B, val flag: Boolean = true)


fun foo(c: C, b: B, bn: B?) {
    val x = A(c)
    val y = A(b)
    val z = A(bn)
}