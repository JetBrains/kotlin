// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test
var s: Int = 1;

inline fun Int.inlineMethod() : Int {
    noInlineLambda()
    return noInlineLambda()
}

inline fun Int.noInlineLambda() =  { s++ } ()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*
fun test1(): Int {
    return 1.inlineMethod()
}

fun box(): String {
    val result = test1()
    if (result != 2) return "test1: ${result}"

    return "OK"
}
