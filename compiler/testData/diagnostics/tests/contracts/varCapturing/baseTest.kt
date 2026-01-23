// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK

private fun testNotCaptured() {
    Thread {
        var another = "hello"
        println(another)
    }
}

private fun testRepeated() {
    var repeat = true
    var attempts = 0
    while (repeat) {
        Thread {
            try {
                println(attempts)
                repeat = false
            } catch (e: Throwable) {
                println(e)
            }
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, javaFunction, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, stringLiteral, tryExpression, whileLoop */
