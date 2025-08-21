// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-50134
// WITH_STDLIB
// FULL_JDK

import java.util.stream.Collectors

fun foo(){
    listOf("").stream().collect(
        Collectors.groupingBy(
            { it },
            Collectors.collectingAndThen(
                Collectors.counting<String>(),
                Long::toInt
            )
        )
    )
}

/* GENERATED_FIR_TAGS: callableReference, flexibleType, functionDeclaration, inProjection, javaFunction, lambdaLiteral,
outProjection, samConversion, starProjection, stringLiteral */
