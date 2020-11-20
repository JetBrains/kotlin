// !CHECK_TYPE
val x get(): String = foo()
val y get(): List<Int> = bar()
val z get(): List<Int> {
    return bar()
}

val u get(): String = <!UNRESOLVED_REFERENCE!>field<!>

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!


fun baz() {
    x checkType { _<String>() }
    y checkType { _<List<Int>>() }
    z checkType { _<List<Int>>() }
}
