// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.IntStream

fun main() {
    val xs = IntStream.range(0, 10).mapToObj { it.toString() }
            .collect(Collectors.toList())
    xs[0]
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction, lambdaLiteral, localProperty,
outProjection, propertyDeclaration, samConversion, starProjection */
