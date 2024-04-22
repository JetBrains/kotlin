class In<in K>

fun <E> intersect(vararg x: In<E>): E = null as E

fun box(): String {
    return if (intersect(In<Int>(), In<String>()) == null) "OK" else "NOT_OK"
}
