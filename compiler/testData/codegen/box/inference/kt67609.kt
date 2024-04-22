// IGNORE_BACKEND_K1: ANY
// Ignore Wasm and Native because they fail at runtime KT-67691
// IGNORE_BACKEND_K2: WASM, NATIVE
class In<in K>

fun <E> intersect(vararg x: In<E>): E = null as E

fun box(): String {
    val a = intersect(In<Int>(), In<String>()) // K2: INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING
    val b = intersect(In<Int>(), *arrayOf(In<String>())) // K2: INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING
    val c = intersect(x = arrayOf(In<Int>(), In<String>())) // K1: TYPE_MISMATCH error. K2: INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING
    val d = intersect(x = *arrayOf(In<Int>(), In<String>())) // K1: TYPE_MISMATCH error. K2: INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING
    val e = intersect(In<Int>(), *arrayOf(In<String>()), In<Long>()) // K2: INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING
    val f = intersect(*arrayOf(In<Int>()), In<String>(), *arrayOf(In<Long>())) // K2: INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING
    return if (a == null && a == b && b == c && c == d && d == e && e == f) "OK" else "NOT_OK"
}
