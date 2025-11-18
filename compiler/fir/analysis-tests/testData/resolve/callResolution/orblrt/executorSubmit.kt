// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// WITH_STDLIB
// ISSUE: KT-7052

import java.util.concurrent.Executors

fun main() {
    val executorService = Executors.newWorkStealingPool()
    val future1 = executorService.submit { "OK" }
    val future2 = executorService.submit { println(123) }

    <!DEBUG_INFO_EXPRESSION_TYPE("(java.util.concurrent.Future<(kotlin.String..kotlin.String?)>..java.util.concurrent.Future<(kotlin.String..kotlin.String?)>?)")!>future1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(java.util.concurrent.Future<*>..java.util.concurrent.Future<*>?)")!>future2<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, lambdaLiteral, localProperty,
propertyDeclaration, samConversion, starProjection, stringLiteral */
