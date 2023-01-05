// WITH_STDLIB
// SKIP_TXT

fun <T> foo(p: VArray<T>) {
    var y: VArray<T>? = null
}

class A<T>(val fieldT: VArray<T>) {
    var fieldTNullable: VArray<T>? = null
}

inline fun <reified T, R> bar(p1: VArray<T>, p2: VArray<R>, p3: VArray<Array<T>>, p4: VArray<Array<R>>, p5: VArray<Array<*>>) {
    var y: VArray<T>
    var z: VArray<Int>
}