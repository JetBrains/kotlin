// !CHECK_TYPE
class A {
    val x get() = 1
    val y get() = id(1)
    val y2 get() = id(id(2))
    val z get() = l("")
    val z2 get() = l(id(l("")))

    val <T> T.u get() = id(this)
}
fun <E> id(x: E) = x
fun <E> l(x: E): List<E> = null!!

fun foo(a: A) {
    a.x checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.y checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.y2 checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.z checkType { <!UNRESOLVED_REFERENCE!>_<!><List<String>>() }
    a.z2 checkType { <!UNRESOLVED_REFERENCE!>_<!><List<List<String>>>() }

    with(a) {
        1.u checkType { _<Int>() }
    }
}
