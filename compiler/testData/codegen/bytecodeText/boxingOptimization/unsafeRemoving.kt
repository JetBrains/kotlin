fun returningBoxed() : Int? = 1
fun acceptingBoxed(x : Int?) : Int ? = x

class A(var x : Int? = null)

fun foo() {
    val rb = returningBoxed()
    acceptingBoxed(2)

    val a = A()
    a.x = 3

    val b = Array<Int?>(4, { null })
    b[100] = 5

    val x = 6 : Int?
    val hc = x!!.hashCode()

    val y = 7 : Int?
    val z = 8 : Int?
    val res = y.identityEquals(z)

    val c1: Any = if (1 == 1) 0 else "abc"
    val c2: Any = if (1 != 1) 0 else "abc"
}

// 10 java/lang/Integer.valueOf
// 1 intValue
