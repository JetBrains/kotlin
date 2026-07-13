// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// WITH_EXTRA_CHECKERS
// LANGUAGE: +ReportEscapingCapturedVariable
// -Xescaping-functions=+my.pkg.MyExecutor.run

package my.pkg

class MyExecutor {
    fun run(block: () -> Unit) {
        block()
    }
}

fun viaCustomExecutor(executor: MyExecutor) {
    var unstable = ""
    executor.run {
        println(unstable)
    }
    unstable = "hello"
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */
