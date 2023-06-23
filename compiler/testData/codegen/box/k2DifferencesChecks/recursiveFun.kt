// ORIGINAL: /compiler/testData/diagnostics/tests/inference/recursiveLocalFuns/recursiveFun.fir.kt
// WITH_STDLIB
fun foo() {
    fun bar() = (fun() = bar())
}


fun box() = "OK".also { foo() }
