fun foo<T>(t: T) = t

fun test(map: MutableMap<Int, Int>, t: Int) {
    map [t] = foo(t) // t was marked with black square
}

//from library
fun <K, V> MutableMap<K, V>.set(key : K, value : V) : V<!BASE_WITH_NULLABLE_UPPER_BOUND!>?<!> = this.put(key, value)