// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +UnionTypes
// DUMP_IR

fun <T> select(a: T, b: T) : T = a

fun box(): String {
    val x = select("OK", 1L)
    return when (x) {
        is String -> x
        is Long -> "Fail"
    }
}