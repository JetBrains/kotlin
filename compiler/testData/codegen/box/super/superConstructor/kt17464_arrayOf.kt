open class A(val array: Array<Any>)

class B : A(arrayOf("OK"))

fun box() = B().array[0].toString()