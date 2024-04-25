// CHECK_TYPE

data class A(val foo: Int)

operator fun A.component1(): String = ""

fun test(a: A) {
    val (b) = a
    b checkType { _<Int>() }
}