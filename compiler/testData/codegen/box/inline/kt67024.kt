// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
inline fun <reified T> id(x: T) = x

inline fun test2(block: (String) -> String = ::id)  = block("K")

// FILE: main.kt
fun test1(block: (String) -> String = ::id)  = block("O")

fun box() : String {
    return test1() + test2()
}
