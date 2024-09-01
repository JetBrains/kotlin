// MODULE: lib
// FILE: A.kt
class A {
    private val privateVal = 21
    private val privateValFunctional = { 21 }

    internal inline fun executor(param: Int = privateVal) = param
    internal inline fun executorFunctional(noinline block: () -> Int = privateValFunctional): Int = block()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += executor()
        result += executorFunctional()
    }
    if (result != 42) return result.toString()
    return "OK"
}
