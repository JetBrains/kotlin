// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// UNSTABLE_CAPTURE: +my.pkg.MyExecutor.run

package my.pkg

class MyExecutor {
    fun run(block: () -> Unit) {
        block()
    }
}

fun viaCustomExecutor(executor: MyExecutor) {
    var unstable = ""
    executor.run {
        println(<!CV_DIAGNOSTIC!>unstable<!>)
    }
    unstable = "hello"
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
