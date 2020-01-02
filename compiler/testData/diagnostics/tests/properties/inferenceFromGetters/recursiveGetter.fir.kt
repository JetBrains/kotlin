// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// NI_EXPECTED_FILE

val x get() = x

class A {
    val y get() = y

    val a get() = b
    val b get() = a

    val z1 get() = <!UNRESOLVED_REFERENCE!>id<!>(z1)
    val z2 get() = <!UNRESOLVED_REFERENCE!>l<!>(z2)

    val u get() = field
}

fun <E> id(x: E) = x
fun <E> l(x: E): List<E> = null!!
