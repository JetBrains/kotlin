// RUN_PIPELINE_TILL: BACKEND
// WITH_EXPERIMENTAL_CHECKERS

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: <!REDUNDANT_EXPLICIT_TYPE!>Long<!> = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
