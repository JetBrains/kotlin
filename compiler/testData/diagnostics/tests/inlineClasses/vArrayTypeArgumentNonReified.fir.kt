// WITH_STDLIB
// SKIP_TXT

fun <T> foo(p: VArray<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>) {
    var y: VArray<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>? = null
}

class A<T>(val fieldT: VArray<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>) {
    var fieldTNullable: VArray<<!TYPE_PARAMETER_AS_REIFIED!>T<!>>? = null
}

inline fun <reified T, R> bar(p1: VArray<T>, p2: VArray<<!TYPE_PARAMETER_AS_REIFIED!>R<!>>, p3: VArray<Array<T>>, p4: VArray<<!TYPE_PARAMETER_AS_REIFIED_ARRAY_ERROR!>Array<R><!>>, p5: VArray<Array<*>>) {
    var y: VArray<T>
    var z: VArray<Int>
}