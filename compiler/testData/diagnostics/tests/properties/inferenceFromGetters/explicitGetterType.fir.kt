// !CHECK_TYPE
val x get(): String = foo()
val y get(): List<Int> = bar()
val z get(): List<Int> {
    return bar()
}

val u get(): String = field

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!


fun baz() {
    x checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    y checkType { <!UNRESOLVED_REFERENCE!>_<!><List<Int>>() }
    z checkType { <!UNRESOLVED_REFERENCE!>_<!><List<Int>>() }
}
