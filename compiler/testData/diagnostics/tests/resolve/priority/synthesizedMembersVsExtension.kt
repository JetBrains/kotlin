// !CHECK_TYPE

data class A(val foo: Int)

operator fun A.<!EXTENSION_SHADOWED_BY_MEMBER!>component1<!>(): String = ""

fun test(a: A) {
    val (b) = a
    b checkType { _<Int>() }
}