// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// ^^^ To be fixed in KT-72862: No function found for symbol
// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: a.kt
private val privateVal = 21
private val privateValFunctional = { 21 }

internal inline fun executor(param: Int = privateVal) = param
internal inline fun executorFunctional(noinline block: () -> Int = privateValFunctional): Int = block()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += executor()
    result += executorFunctional()
    if (result != 42) return result.toString()
    return "OK"
}
