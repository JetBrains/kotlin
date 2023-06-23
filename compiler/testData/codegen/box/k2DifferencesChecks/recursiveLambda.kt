// ORIGINAL: /compiler/testData/diagnostics/tests/inference/recursiveLocalFuns/recursiveLambda.fir.kt
// WITH_STDLIB
fun foo() {
    fun bar() = {
        bar()
    }
}

fun box() = "OK".also { foo() }
