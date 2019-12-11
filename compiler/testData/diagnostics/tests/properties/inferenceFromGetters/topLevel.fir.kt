// !CHECK_TYPE
val x get() = 1
val y get() = id(1)
val y2 get() = id(id(2))
val z get() = l("")
val z2 get() = l(id(l("")))

val <T> T.u get() = id(this)

fun <E> id(x: E) = x
fun <E> l(x: E): List<E> = null!!

fun foo() {
    x checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    y checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    y2 checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    z checkType { <!UNRESOLVED_REFERENCE!>_<!><List<String>>() }
    z2 checkType { <!UNRESOLVED_REFERENCE!>_<!><List<List<String>>>() }

    1.u checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
}
