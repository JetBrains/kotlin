// NO_CHECK_LAMBDA_INLINING
// FILE: a.kt
private val privateVal = 21
private val privateValFunctional = { 21 }

internal inline fun executor(param: Int = privateVal) = param
internal inline fun executorFunctional(noinline block: () -> Int = privateValFunctional): Int = block()

// FILE: main.kt
fun box(): String {
    var result = 0
    result += executor()
    result += executorFunctional()
    if (result != 42) return result.toString()
    return "OK"
}
