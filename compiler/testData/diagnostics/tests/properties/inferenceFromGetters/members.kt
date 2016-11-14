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
fun <E> l(<!UNUSED_PARAMETER!>x<!>: E): List<E> = null!!

fun foo(a: A) {
    a.x checkType { _<Int>() }
    a.y checkType { _<Int>() }
    a.y2 checkType { _<Int>() }
    a.z checkType { _<List<String>>() }
    a.z2 checkType { _<List<List<String>>>() }

    with(a) {
        1.u checkType { _<Int>() }
    }
}
