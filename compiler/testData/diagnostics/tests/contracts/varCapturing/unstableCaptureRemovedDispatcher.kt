// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK
// UNSTABLE_CAPTURE: -java.lang.Thread.Thread

fun viaThreadNotReported() {
    var unstable = ""
    Thread {
        println(unstable)
    }
    unstable = "hello"
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, javaFunction, lambdaLiteral, localProperty, propertyDeclaration,
samConversion, stringLiteral */
