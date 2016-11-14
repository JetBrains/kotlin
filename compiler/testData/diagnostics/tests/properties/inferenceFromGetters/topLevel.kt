// !CHECK_TYPE
val x get() = 1
val y get() = id(1)
val y2 get() = id(id(2))
val z get() = l("")
val z2 get() = l(id(l("")))

val <T> T.u get() = id(this)

fun <E> id(x: E) = x
fun <E> l(<!UNUSED_PARAMETER!>x<!>: E): List<E> = null!!

fun foo() {
    x checkType { _<Int>() }
    y checkType { _<Int>() }
    y2 checkType { _<Int>() }
    z checkType { _<List<String>>() }
    z2 checkType { _<List<List<String>>>() }

    1.u checkType { _<Int>() }
}
