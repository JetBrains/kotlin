// WITH_STDLIB
// SKIP_TXT

fun <T> foo(p: VArray<T>) {
    var y: VArray<T>? = null
}

class A<T>(val fieldT: VArray<T>) {
    var fieldTNullable: VArray<T>? = null
}

inline fun <reified T> bar(p: VArray<T>) {
    var y: VArray<T>
    var z: VArray<Int>
}