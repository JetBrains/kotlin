open class B {
    val x = 1
}

class A : B() {
    fun getX() = 1

    fun getA(): Int = 1
    val a: Int = 1
}

fun getB(): Int = 1
val b: Int = 1

trait Tr {
    fun getTr() = 1
}

class SubTr : Tr {
    val tr = 1
}
