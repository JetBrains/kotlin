// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// FULL_JDK
// LANGUAGE: -DontMakeExplicitNullableJavaTypeArgumentsFlexible

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, Boolean?> {
        it != ""
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, inProjection, javaFunction, lambdaLiteral,
outProjection, samConversion, stringLiteral */
