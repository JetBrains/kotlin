fun returningBoxed() : Int? = 1
fun acceptingBoxed(x : Int?) : Int ? = x

class A(var x : Int? = null)

val one = 1

fun foo() {
    val rb = returningBoxed()
    acceptingBoxed(2)

    val a = A()
    a.x = 3

    val b = arrayOfNulls<Int>(4)
    b[100] = 5

    val y: Int? = 7
    val z: Int? = 8
    val res = y === z

    val c1: Any = if (1 == one) 0 else "abc"
    val c2: Any = if (1 != one) 0 else "abc"
}

// 8 java/lang/Integer.valueOf
// 0 intValue
